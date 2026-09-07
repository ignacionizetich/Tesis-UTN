import { Injectable } from '@angular/core';
import User from '../../models/users';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { SessionCleanupService } from '../session-cleanup/session-cleanup.service';
import { SessionStore } from '../../core/session/session.store';
import {
  ApiMessageResponse,
  LoginResponse,
  RefreshTokenResponse,
  RegisterResponse,
} from '../../models/auth';
import { httpStatus } from '../../shared/utils/error-message';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private baseUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private sessionCleanup: SessionCleanupService,
    private sessionStore: SessionStore
  ) {}

  registerUser(user: User): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.baseUrl}/user/create`, user);
  }

  /** Login HTTP sin persistir (tests / casos especiales). */
  loginUser(credentials: { username: string; password: string }): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/auth/login`, credentials, {
      withCredentials: true
    });
  }

  /** Login + limpia cache en memoria + persiste JWT/accountId/role. */
  loginAndPersist(credentials: { username: string; password: string }): Observable<LoginResponse> {
    return this.loginUser(credentials).pipe(
      tap((response) => {
        this.sessionCleanup.clearAll();
        this.sessionStore.setSession({
          accessToken: response.accessToken,
          accountId: response.accountId,
          role: response.role
        });
      })
    );
  }

  isLoggedIn(): boolean {
    return this.sessionStore.hasAccessToken();
  }

  sendRecoverMail(email: string): Observable<ApiMessageResponse> {
    return this.http.post<ApiMessageResponse>(`${this.baseUrl}/auth/send-recover-mail`, { email });
  }

  refreshToken(): Observable<RefreshTokenResponse> {
    return this.http.post<RefreshTokenResponse>(`${this.baseUrl}/auth/refresh`, {}, {
      withCredentials: true
    }).pipe(
      catchError((error: unknown) => {
        const status = httpStatus(error);
        if (status === 401 || status === 498) {
          this.clearLocalSession();
        }
        return throwError(() => error);
      })
    );
  }

  logoutUser(): Observable<ApiMessageResponse> {
    return this.http.post<ApiMessageResponse>(`${this.baseUrl}/auth/logout`, {}, {
      withCredentials: true
    });
  }

  hasValidSession(): boolean {
    return this.sessionStore.hasSession();
  }

  clearLocalSession(): void {
    this.sessionCleanup.clearAll();
  }
}
