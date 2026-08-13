import { Injectable } from '@angular/core';
import UserData from '../../models/user-data';
import { UserResponse } from '../../models/admin.interface';

/** Keys históricas de localStorage — se mantienen para no invalidar sesiones abiertas. */
const KEYS = {
  accessToken: 'JWT',
  accountId: 'accountId',
  role: 'role',
  userData: 'userData',
  adminUsers: 'usuariosAdmin',
  /** Legacy: casi nunca se escribe; admin lo lee como fallback. */
  legacyUserId: 'userId',
} as const;

export interface AuthSessionSnapshot {
  accessToken: string;
  accountId: string;
  role: string;
}

/**
 * Único dueño de la persistencia de sesión autenticada.
 * Pages/forms/guards no deben leer/escribir estas keys directo.
 */
@Injectable({
  providedIn: 'root',
})
export class SessionStore {
  getAccessToken(): string | null {
    return localStorage.getItem(KEYS.accessToken);
  }

  setAccessToken(token: string): void {
    localStorage.setItem(KEYS.accessToken, token);
  }

  getAccountId(): string | null {
    return localStorage.getItem(KEYS.accountId);
  }

  getRole(): string | null {
    return localStorage.getItem(KEYS.role);
  }

  isAdmin(): boolean {
    return this.getRole() === 'ADMIN';
  }

  hasAccessToken(): boolean {
    return !!this.getAccessToken();
  }

  hasSession(): boolean {
    return !!(this.getAccessToken() && this.getAccountId());
  }

  /** Persiste tokens de login. No toca userData (se hidrata después vía API). */
  setSession(session: {
    accessToken: string;
    accountId: string | number;
    role: string;
  }): void {
    localStorage.setItem(KEYS.accessToken, session.accessToken);
    localStorage.setItem(KEYS.accountId, String(session.accountId));
    localStorage.setItem(KEYS.role, session.role);
  }

  getUserData(): UserData | null {
    const raw = localStorage.getItem(KEYS.userData);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as UserData;
    } catch {
      localStorage.removeItem(KEYS.userData);
      return null;
    }
  }

  setUserData(userData: UserData | null): void {
    if (userData) {
      localStorage.setItem(KEYS.userData, JSON.stringify(userData));
    } else {
      localStorage.removeItem(KEYS.userData);
    }
  }

  getAdminUsersCache(): UserResponse[] | null {
    const raw = localStorage.getItem(KEYS.adminUsers);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as UserResponse[];
    } catch {
      localStorage.removeItem(KEYS.adminUsers);
      return null;
    }
  }

  setAdminUsersCache(users: UserResponse[]): void {
    localStorage.setItem(KEYS.adminUsers, JSON.stringify(users));
  }

  clearAdminUsersCache(): void {
    localStorage.removeItem(KEYS.adminUsers);
  }

  /**
   * Hint de identidad para UI admin (evitar auto-disable).
   * Conserva el orden legacy: userData → userId → accountId.
   */
  getCurrentUserIdHint(): number {
    const userData = this.getUserData() as (UserData & {
      id?: number;
      userId?: number;
    }) | null;

    if (userData) {
      const fromUserData =
        Number(userData.id) ||
        Number(userData.userId) ||
        Number(userData.idAccount) ||
        0;
      if (fromUserData > 0) {
        return fromUserData;
      }
    }

    const legacyUserId = localStorage.getItem(KEYS.legacyUserId);
    if (legacyUserId) {
      const parsed = parseInt(legacyUserId, 10) || 0;
      if (parsed > 0) {
        return parsed;
      }
    }

    const accountId = this.getAccountId();
    if (accountId) {
      const parsed = parseInt(accountId, 10) || 0;
      if (parsed > 0) {
        return parsed;
      }
    }

    return 0;
  }

  /** Limpia keys de auth/sesión. No toca theme ni caches arcash_*. */
  clear(): void {
    localStorage.removeItem(KEYS.accessToken);
    localStorage.removeItem(KEYS.accountId);
    localStorage.removeItem(KEYS.role);
    localStorage.removeItem(KEYS.userData);
    localStorage.removeItem(KEYS.adminUsers);
  }
}
