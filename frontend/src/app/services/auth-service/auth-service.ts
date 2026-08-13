import { Injectable } from '@angular/core';
import User from '../../models/users';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { SessionCleanupService } from '../session-cleanup/session-cleanup.service';
import { SessionStore } from '../../core/session/session-store';

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

  registerUser(user: User) {
    return this.http.post<any>(`${this.baseUrl}/user/create`, user);
  }

  /** Login HTTP sin persistir (tests / casos especiales). */
  loginUser(credentials: { username: string; password: string }) {
    return this.http.post<any>(`${this.baseUrl}/auth/login`, credentials, {
      withCredentials: true
    });
  }

  /** Login + limpia cache en memoria + persiste JWT/accountId/role. */
  loginAndPersist(credentials: { username: string; password: string }): Observable<any> {
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

  sendRecoverMail(email: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/auth/send-recover-mail`, { email });
  }

  refreshToken(): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/auth/refresh`, {}, {
      withCredentials: true
    }).pipe(
      catchError((error: any) => {
        if (error.status === 401 || error.status === 498) {
          this.clearLocalSession();
        }
        return throwError(() => error);
      })
    );
  }

  logoutUser(): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/auth/logout`, {}, {
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
