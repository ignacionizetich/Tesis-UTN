import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth/auth.service';
import { SessionStore } from '../core/session/session.store';
import { inject } from '@angular/core';

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const sessionStore = inject(SessionStore);
  const router = inject(Router);

  if (authService.isLoggedIn() && sessionStore.isAdmin()) {
    return true;
  } else {
    return router.parseUrl('/dashboard');
  }
};
