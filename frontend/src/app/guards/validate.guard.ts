import { inject } from '@angular/core';
import { CanActivateFn, ActivatedRouteSnapshot, Router } from '@angular/router';
import { logger } from '../shared/utils/logger';

export const validateGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const router = inject(Router);
  
  const token = route.queryParams['token'];
  
  if (!token || token.trim() === '') {
    // Si no hay token o está vacío, redirigir a 404
    logger.warn('Intento de acceso directo a /validate sin token. Redirigiendo a 404.');
    router.navigate(['/404']);
    return false;
  }
  
  // Si hay token, permitir el acceso (el componente hará la validación)
  return true;
};
