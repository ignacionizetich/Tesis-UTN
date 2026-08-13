import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { lastValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CardAuditEvent,
  CardUnlockResponse,
  VirtualCardListResponse,
  VirtualCardReveal,
  VirtualCardSummary,
} from '../../models/virtual-card';
import { logger } from '../../shared/utils/logger';

@Injectable({
  providedIn: 'root',
})
export class VirtualCardApi {
  private readonly baseUrl = `${environment.apiUrl}/cards`;
  private unlockToken: string | null = null;
  private unlockExpiresAt = 0;

  constructor(private http: HttpClient) {}

  isUnlocked(): boolean {
    return !!this.unlockToken && Date.now() < this.unlockExpiresAt;
  }

  clearUnlock(): void {
    this.unlockToken = null;
    this.unlockExpiresAt = 0;
  }

  getUnlockToken(): string | null {
    return this.isUnlocked() ? this.unlockToken : null;
  }

  async listCards(): Promise<VirtualCardListResponse> {
    return lastValueFrom(this.http.get<VirtualCardListResponse>(this.baseUrl));
  }

  async setPin(pin: string, confirmPin: string, currentPin?: string): Promise<CardUnlockResponse> {
    const body: Record<string, string> = { pin, confirmPin };
    if (currentPin) {
      body['currentPin'] = currentPin;
    }
    const response = await lastValueFrom(
      this.http.post<CardUnlockResponse>(`${this.baseUrl}/pin`, body)
    );
    this.storeUnlock(response);
    return response;
  }

  async verifyPin(pin: string): Promise<CardUnlockResponse> {
    const response = await lastValueFrom(
      this.http.post<CardUnlockResponse>(`${this.baseUrl}/pin/verify`, { pin })
    );
    this.storeUnlock(response);
    return response;
  }

  async reveal(cardId: number): Promise<VirtualCardReveal> {
    const token = this.getUnlockToken();
    if (!token) {
      throw new Error('PIN requerido');
    }
    const headers = new HttpHeaders({ 'X-Card-Unlock': token });
    return lastValueFrom(
      this.http.get<VirtualCardReveal>(`${this.baseUrl}/${cardId}/reveal`, { headers })
    );
  }

  async updateStatus(cardId: number, status: 'ACTIVE' | 'PAUSED'): Promise<VirtualCardSummary> {
    return lastValueFrom(
      this.http.patch<VirtualCardSummary>(`${this.baseUrl}/${cardId}/status`, { status })
    );
  }

  async updateLimit(cardId: number, dailyLimit: number): Promise<VirtualCardSummary> {
    return lastValueFrom(
      this.http.patch<VirtualCardSummary>(`${this.baseUrl}/${cardId}/limit`, { dailyLimit })
    );
  }

  async cancel(cardId: number): Promise<VirtualCardSummary> {
    return lastValueFrom(this.http.post<VirtualCardSummary>(`${this.baseUrl}/${cardId}/cancel`, {}));
  }

  async reissue(cardId: number): Promise<VirtualCardSummary> {
    return lastValueFrom(this.http.post<VirtualCardSummary>(`${this.baseUrl}/${cardId}/reissue`, {}));
  }

  async getAudit(): Promise<CardAuditEvent[]> {
    return lastValueFrom(this.http.get<CardAuditEvent[]>(`${this.baseUrl}/audit`));
  }

  private storeUnlock(response: CardUnlockResponse): void {
    if (response.success && response.unlockToken) {
      this.unlockToken = response.unlockToken;
      const ttlMs = (response.expiresInSeconds || 300) * 1000;
      this.unlockExpiresAt = Date.now() + ttlMs;
    }
  }

  handleError(error: unknown, fallback: string): string {
    logger.error(fallback, error);
    const err = error as { error?: { message?: string; error?: string }; message?: string };
    return err?.error?.message || err?.error?.error || err?.message || fallback;
  }
}
