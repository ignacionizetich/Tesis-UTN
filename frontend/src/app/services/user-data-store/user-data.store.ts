import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, lastValueFrom } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import UserData from '../../models/user-data';
import { environment } from '../../../environments/environment';
import { SessionStore } from '../../core/session/session.store';
import { logger } from '../../shared/utils/logger';

/**
 * Estado + API de perfil de usuario autenticado (userData$).
 * No conoce transfers/taxes/favorites.
 */
@Injectable({
  providedIn: 'root',
})
export class UserDataStore {
  private readonly baseUrl = environment.apiUrl;

  private readonly userDataSubject = new BehaviorSubject<UserData | null>(null);
  readonly userData$ = this.userDataSubject.asObservable();

  constructor(
    private http: HttpClient,
    private sessionStore: SessionStore
  ) {
    this.hydrateFromSession();
  }

  clear(): void {
    this.userDataSubject.next(null);
    this.persist(null);
  }

  getCurrent(): UserData | null {
    return this.userDataSubject.getValue();
  }

  load(forceRefresh: boolean = false): Observable<UserData | null> {
    const accountId = this.sessionStore.getAccountId();
    const jwt = this.sessionStore.getAccessToken();

    if (!accountId || !jwt) {
      logger.error('>>> UserDataStore.load: No hay sesión válida (falta accountId o JWT).');
      if (this.userDataSubject.getValue() !== null) {
        this.userDataSubject.next(null);
        this.persist(null);
      }
      return of(null);
    }

    const currentValue = this.userDataSubject.getValue();
    const cacheMatchesSession = !!currentValue && currentValue.idAccount === accountId;
    if (!forceRefresh && cacheMatchesSession) {
      return of(currentValue);
    }

    if (currentValue && !cacheMatchesSession) {
      this.userDataSubject.next(null);
      this.persist(null);
    }

    return this.http
      .get<any>(`${this.baseUrl}/user/data`, {
        headers: {
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          Pragma: 'no-cache',
          Expires: '0',
        },
      })
      .pipe(
        tap((response) => {
          if (response) {
            const userData: UserData = {
              name: response.name || 'Usuario',
              lastName: response.lastName || '',
              dni: response.dni || '',
              email: response.email || '',
              alias: response.alias || 'usuario.alias',
              cvu: response.cvu || '0000000000000000000000',
              username: response.username || 'usuario',
              balance: response.balance || 0,
              idAccount: response.idAccount?.toString() || accountId,
            };
            this.userDataSubject.next(userData);
            this.persist(userData);
          } else {
            logger.warn('>>> UserDataStore.load: Respuesta del backend vacía.');
            this.userDataSubject.next(null);
            this.persist(null);
          }
        }),
        catchError((error) => {
          logger.error('>>> UserDataStore.load: ERROR en GET /user/data:', error);
          this.userDataSubject.next(null);
          this.persist(null);
          return of(null);
        })
      );
  }

  async updateAlias(newAlias: string): Promise<void> {
    const accountId = this.sessionStore.getAccountId();
    if (!accountId) {
      throw new Error('No hay sesión activa');
    }
    try {
      await lastValueFrom(
        this.http.put(`${this.baseUrl}/accounts/${accountId}/changeAlias`, { newAlias })
      );
      this.load(true).subscribe();
    } catch (error) {
      logger.error('Error updating alias:', error);
      throw error;
    }
  }

  async updateUsername(newUsername: string): Promise<any> {
    if (!this.sessionStore.hasAccessToken()) {
      throw new Error('No hay sesión activa');
    }
    try {
      const response = await lastValueFrom(
        this.http.put(`${this.baseUrl}/auth/changeUsername`, { newUsername })
      );
      this.load(true).subscribe();
      return response;
    } catch (error) {
      logger.error('Error updating username:', error);
      throw error;
    }
  }

  async checkSession(): Promise<boolean> {
    if (!this.sessionStore.hasAccessToken()) {
      return false;
    }
    try {
      const response = (await lastValueFrom(
        this.http.get<any>(`${this.baseUrl}/auth/check-session`)
      )) as any;
      return response?.status === 'ACTIVE';
    } catch (error) {
      logger.error('Error verificando sesión:', error);
      return false;
    }
  }

  private hydrateFromSession(): void {
    try {
      if (!this.sessionStore.hasSession()) {
        return;
      }

      const accountId = this.sessionStore.getAccountId();
      const userData = this.sessionStore.getUserData();
      if (!accountId || !userData) {
        return;
      }

      if (userData.idAccount && userData.idAccount !== accountId) {
        this.sessionStore.setUserData(null);
        return;
      }

      if (!this.userDataSubject.getValue()) {
        this.userDataSubject.next(userData);
      }
    } catch (error) {
      logger.error('>>> UserDataStore.hydrateFromSession: ERROR:', error);
      this.sessionStore.setUserData(null);
      if (this.userDataSubject.getValue() !== null) {
        this.userDataSubject.next(null);
      }
    }
  }

  private persist(userData: UserData | null): void {
    try {
      this.sessionStore.setUserData(userData);
    } catch (error) {
      logger.error('>>> UserDataStore.persist: ERROR:', error);
    }
  }
}
