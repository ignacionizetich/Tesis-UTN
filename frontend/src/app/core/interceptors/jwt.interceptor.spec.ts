import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { jwtInterceptor } from './jwt.interceptor';
import { AuthService } from '../../services/auth/auth.service';
import { SessionStore } from '../session/session.store';
import { ToastService } from '../../services/toast/toast.service';
import { ApiErrorCodes } from '../../models/api-error';

describe('jwtInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;
  let toast: jasmine.SpyObj<ToastService>;

  const API = '/api/accounts/user-accounts';

  /** Cuerpo de error tal como lo arma el ErrorResponse del backend. */
  const errorBody = (code: string, message: string, status: number) => ({
    success: false,
    status,
    code,
    message,
    traceId: 'trace-test',
  });

  beforeEach(() => {
    authService = jasmine.createSpyObj('AuthService', [
      'refreshToken',
      'logoutUser',
      'clearLocalSession',
    ]);
    router = jasmine.createSpyObj('Router', ['navigate']);
    toast = jasmine.createSpyObj('ToastService', ['show']);

    authService.logoutUser.and.returnValue(of({ success: true, message: 'Sesión cerrada' }));

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
        { provide: ToastService, useValue: toast },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);

    // El interceptor solo adjunta el Bearer si hay token guardado.
    TestBed.inject(SessionStore).setAccessToken('token-vigente');
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('adjunta el access token en las request autenticadas', () => {
    http.get(API).subscribe();

    const req = httpMock.expectOne(API);
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-vigente');
    req.flush({});
  });

  it('ante ACCOUNT_DISABLED cierra la sesión sin intentar renovar el token', (done) => {
    // El token es válido: el problema es la cuenta, así que renovarlo fallaría igual.
    http.get(API).subscribe({
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(401);
        expect(authService.refreshToken).not.toHaveBeenCalled();
        expect(authService.clearLocalSession).toHaveBeenCalled();
        expect(router.navigate).toHaveBeenCalledWith(['/login']);
        // Sin el aviso el usuario aparece en el login sin saber por qué.
        expect(toast.show).toHaveBeenCalledWith('Tu cuenta está deshabilitada.', 'error');
        done();
      },
    });

    httpMock.expectOne(API).flush(
      errorBody(ApiErrorCodes.ACCOUNT_DISABLED, 'Tu cuenta está deshabilitada.', 401),
      { status: 401, statusText: 'Unauthorized' }
    );
  });

  it('ante un 401 por token vencido renueva y reintenta la request original', (done) => {
    authService.refreshToken.and.returnValue(of({ accessToken: 'token-nuevo' }));

    http.get(API).subscribe({
      next: (body) => {
        expect(body).toEqual({ ok: true });
        expect(authService.refreshToken).toHaveBeenCalled();
        expect(router.navigate).not.toHaveBeenCalled();
        done();
      },
    });

    httpMock.expectOne(API).flush(
      errorBody('INVALID_TOKEN', 'El token expiró', 401),
      { status: 401, statusText: 'Unauthorized' }
    );

    // El reintento tiene que salir con el token nuevo, no con el vencido.
    const retry = httpMock.expectOne(API);
    expect(retry.request.headers.get('Authorization')).toBe('Bearer token-nuevo');
    retry.flush({ ok: true });
  });

  it('no intenta renovar ante errores que no son de autenticación', (done) => {
    http.get(API).subscribe({
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(409);
        expect(authService.refreshToken).not.toHaveBeenCalled();
        expect(router.navigate).not.toHaveBeenCalled();
        done();
      },
    });

    httpMock.expectOne(API).flush(
      errorBody('CONFLICT', 'Ya tenés un préstamo activo', 409),
      { status: 409, statusText: 'Conflict' }
    );
  });
});
