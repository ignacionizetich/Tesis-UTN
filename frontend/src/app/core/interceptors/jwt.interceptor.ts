import { HttpInterceptorFn, HttpErrorResponse, HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../../services/auth/auth.service';
import { SessionStore } from '../session/session.store';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError, BehaviorSubject, filter, take, Observable } from 'rxjs';
import { logger } from '../../shared/utils/logger';
import { apiErrorCode, errorMessage } from '../../shared/utils/error-message';
import { ApiErrorCodes } from '../../models/api-error';
import { RefreshTokenResponse } from '../../models/auth';
import { ToastService } from '../../services/toast/toast.service';

// Variables globales para el estado de refresh
let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const sessionStore = inject(SessionStore);
  const router = inject(Router);
  const toast = inject(ToastService);

  // Endpoints públicos: no adjuntar Bearer.
  // /auth/refresh debe ir sin access token (el BE lo ignora en permitAll, pero
  // un JWT vencido en el header no aporta y confunde el flujo).
  const publicEndpoints = [
    '/auth/login',
    '/auth/refresh',
    '/auth/send-recover-mail',
    '/auth/reset-password',
    '/user/create'
  ];

  const isPublicEndpoint = publicEndpoints.some(endpoint =>
    req.url.includes(endpoint)
  );

  if (isPublicEndpoint) {
    return next(req);
  }

  const token = sessionStore.getAccessToken();

  if (!token) {
    return next(req);
  }

  const reqConToken = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(reqConToken).pipe(
    catchError((error: HttpErrorResponse) => {
      if (req.url.includes('/auth/refresh')) {
        return throwError(() => error);
      }

      // create-admin puede devolver 401 por conflictos de constraint: no refrescar ni logout
      if (error.status === 401 && req.url.includes('/admin/users/create-admin')) {
        return throwError(() => error);
      }

      // Cuenta deshabilitada: el token es válido, el problema es la cuenta. Renovarlo va a
      // fallar igual (el backend revalida el estado en /auth/refresh), así que se corta la
      // sesión de una y se le explica al usuario en lugar de dejarlo en el login sin motivo.
      if (apiErrorCode(error) === ApiErrorCodes.ACCOUNT_DISABLED) {
        toast.show(errorMessage(error, 'Tu cuenta está deshabilitada.'), 'error');
        handleFullLogout(authService, router);
        return throwError(() => error);
      }

      // Access vencido / inválido: el filtro BE responde 401; algunos flujos legacy usan 498.
      // En ambos casos intentamos renovar con la cookie de refresh.
      if (error.status === 401 || error.status === 498) {
        return handleAccessTokenExpired(reqConToken, next, authService, sessionStore, router, toast);
      }

      if (error.status === 409) {
        return throwError(() => error);
      }

      return throwError(() => error);
    })
  );
};

function handleAccessTokenExpired(
  request: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
  sessionStore: SessionStore,
  router: Router,
  toast: ToastService
): Observable<HttpEvent<unknown>> {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    return authService.refreshToken().pipe(
      switchMap((response: RefreshTokenResponse) => {
        isRefreshing = false;

        const newToken = response.accessToken;
        sessionStore.setAccessToken(newToken);
        refreshTokenSubject.next(newToken);

        const newRequest = request.clone({
          setHeaders: {
            Authorization: `Bearer ${newToken}`
          }
        });

        return next(newRequest);
      }),
      catchError((error: unknown) => {
        isRefreshing = false;
        // Si la cuenta quedó deshabilitada mientras la sesión estaba abierta, el refresh
        // también lo informa. Sin este aviso el usuario aparece en el login sin saber por qué.
        if (apiErrorCode(error) === ApiErrorCodes.ACCOUNT_DISABLED) {
          toast.show(errorMessage(error, 'Tu cuenta está deshabilitada.'), 'error');
        }
        handleFullLogout(authService, router);
        return throwError(() => error);
      })
    );
  }

  return refreshTokenSubject.pipe(
    filter(token => token !== null),
    take(1),
    switchMap(token => {
      const newRequest = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      return next(newRequest);
    })
  );
}

function handleFullLogout(authService: AuthService, router: Router): void {
  authService.logoutUser().subscribe({
    next: () => {
      logger.debug('Logout exitoso en backend');
    },
    error: (err) => {
      logger.warn('Error al hacer logout en backend:', err);
    },
    complete: () => {
      authService.clearLocalSession();
      router.navigate(['/login']);
    }
  });
}
