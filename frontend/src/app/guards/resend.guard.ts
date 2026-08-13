import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { logger } from '../shared/utils/logger';

export const resendGuard: CanActivateFn = () => {
  const router = inject(Router);
  
  // Obtener el estado de navegación actual
  const navigation = router.getCurrentNavigation();
  const currentState = window.history.state;
  
  // Verificar si es una navegación interna válida
  const isInternalNavigation = navigation?.previousNavigation !== null || 
                              currentState?.navigationId > 1 ||
                              currentState?.allowResendAccess === true;
  
  // Verificar si viene de una página válida mediante el referrer
  const referrer = document.referrer;
  const validReferrerPaths = ['/login', '/register', '/validate', '/forgot'];
  const isValidReferrer = validReferrerPaths.some(path => 
    referrer.includes(path) || referrer.includes('localhost:4200')
  );
  
  // Verificar si hay un flag específico en sessionStorage
  const hasResendAccess = sessionStorage.getItem('resendAccess') === 'true';
  
  if (!isInternalNavigation && !isValidReferrer && !hasResendAccess) {
    logger.warn('Intento de acceso directo a /resend bloqueado. Redirigiendo a home.');
    router.navigate(['/home']);
    return false;
  }
  
  // Limpiar el flag de acceso después de usarlo
  sessionStorage.removeItem('resendAccess');
  
  return true;
};
