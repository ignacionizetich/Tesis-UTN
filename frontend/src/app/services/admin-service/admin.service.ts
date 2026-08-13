import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserResponse, AdminRequest } from '../../models/admin.interface';
import { environment } from '../../../environments/environment';
import { SessionStore } from '../../core/session/session-store';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = environment.apiUrl + '/admin';

  constructor(
    private http: HttpClient,
    private sessionStore: SessionStore
  ) {}

  checkAccess(): Observable<any> {
    return this.http.get(`${this.apiUrl}/check-access`);
  }

  getAuthenticatedUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.apiUrl}/users`);
  }

  disableUser(userId: number): Observable<string> {
    return this.http.put(`${this.apiUrl}/users/${userId}/disable`, {}, {
      responseType: 'text'
    });
  }

  enableUser(userId: number): Observable<string> {
    return this.http.put(`${this.apiUrl}/users/${userId}/enable`, {}, {
      responseType: 'text'
    });
  }

  createAdmin(adminData: AdminRequest): Observable<string> {
    return this.http.post(`${this.apiUrl}/users/create-admin`, adminData, {
      responseType: 'text'
    });
  }

  isAdmin(): boolean {
    return this.sessionStore.isAdmin();
  }

  getCachedUsers(): UserResponse[] | null {
    return this.sessionStore.getAdminUsersCache();
  }

  cacheUsers(users: UserResponse[]): void {
    this.sessionStore.setAdminUsersCache(users);
  }

  updateUserInCache(userId: number, active: boolean): void {
    const users = this.getCachedUsers();
    if (users) {
      const userIndex = users.findIndex(user => user.id === userId);
      if (userIndex !== -1) {
        users[userIndex].active = active;
        this.cacheUsers(users);
      }
    }
  }
}
