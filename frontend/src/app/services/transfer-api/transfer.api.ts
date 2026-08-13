import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { lastValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SessionStore } from '../../core/session/session-store';
import { UserDataStore } from '../user-data-store/user-data.store';
import { TransactionHistoryStore } from '../transaction-history-store/transaction-history.store';

export interface AccountSearchResult {
  idaccount: string;
  alias: string;
  cvu: string;
  currency: 'ARS' | 'USD';
  user: {
    nombre: string;
    apellido: string;
    dni: string;
  };
}

/** Búsqueda, depósito y transferencia. Refresca perfil/historial al mutar. */
@Injectable({
  providedIn: 'root',
})
export class TransferApi {
  private readonly baseUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private sessionStore: SessionStore,
    private userDataStore: UserDataStore,
    private transactionHistoryStore: TransactionHistoryStore
  ) {}

  async buscarCuenta(input: string): Promise<AccountSearchResult> {
    if (!this.sessionStore.hasAccessToken()) {
      throw new Error('No hay sesión activa');
    }
    try {
      const response = await lastValueFrom(
        this.http.get<AccountSearchResult>(
          `${this.baseUrl}/transactions/search/${encodeURIComponent(input)}`
        )
      );
      if (!response) {
        throw new Error('Cuenta no encontrada');
      }
      return response;
    } catch (error: any) {
      console.error('Error buscando cuenta:', error);
      if (error.status === 404) {
        throw new Error('Cuenta no encontrada');
      }
      if (error.status === 401) {
        throw new Error('Sesión expirada');
      }
      if (error.message === 'Cuenta no encontrada' || error.message === 'Sesión expirada') {
        throw error;
      }
      throw new Error('Error al buscar la cuenta');
    }
  }

  async ingresarDinero(balance: number): Promise<any> {
    const accountId = this.sessionStore.getAccountId();
    if (!accountId) {
      throw new Error('No hay sesión activa');
    }
    try {
      const response = await lastValueFrom(
        this.http.put(`${this.baseUrl}/accounts/${accountId}/balance`, { balance })
      );
      this.userDataStore.load(true).subscribe();
      return response;
    } catch (error) {
      console.error('Error ingresando dinero:', error);
      this.userDataStore.load(true).subscribe();
      throw error;
    }
  }

  async realizarTransferencia(
    idDestino: string,
    monto: number,
    currency: 'ARS' | 'USD'
  ): Promise<any> {
    const accountId = this.sessionStore.getAccountId();
    if (!accountId) {
      throw new Error('No hay sesión activa');
    }
    try {
      const response = await lastValueFrom(
        this.http.post(`${this.baseUrl}/transactions/transfer/${idDestino}`, {
          balance: monto,
          currency,
        })
      );
      this.userDataStore.load(true).subscribe();
      this.transactionHistoryStore
        .load()
        .catch((err) => console.error('Error recargando tx después de transferir:', err));
      return response;
    } catch (error) {
      console.error('Error realizando transferencia:', error);
      this.userDataStore.load(true).subscribe();
      this.transactionHistoryStore
        .load()
        .catch((err) =>
          console.error('Error recargando tx después de transferir (fallida):', err)
        );
      throw error;
    }
  }
}
