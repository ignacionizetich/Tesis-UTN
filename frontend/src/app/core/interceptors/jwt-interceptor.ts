import { HttpInterceptorFn, HttpErrorResponse, HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../../services/auth-service/auth-service';
import { SessionStore } from '../session/session-store';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError, BehaviorSubject, filter, take, Observable } from 'rxjs';

// Variables globales para el estado de refresh
let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const sessionStore = inject(SessionStore);
  const router = inject(Router);

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

      // Access vencido / inválido: el filtro BE responde 401; algunos flujos legacy usan 498.
      // En ambos casos intentamos renovar con la cookie de refresh.
      if (error.status === 401 || error.status === 498) {
        return handleAccessTokenExpired(reqConToken, next, authService, sessionStore, router);
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
  router: Router
): Observable<HttpEvent<unknown>> {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    return authService.refreshToken().pipe(
      switchMap((response: any) => {
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
      catchError((error: any) => {
        isRefreshing = false;
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
      console.log('Logout exitoso en backend');
    },
    error: (err) => {
      console.warn('Error al hacer logout en backend:', err);
    },
    complete: () => {
      authService.clearLocalSession();
      router.navigate(['/login']);
    }
  });
}
