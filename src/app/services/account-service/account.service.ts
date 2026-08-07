import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap, map } from 'rxjs/operators';
import { environment } from '../../../enviroments/enviroment';

export interface Account {
  accountId: string;
  accountAlias: string;
  currency: 'ARS' | 'USD';
  balance: number;
}

export interface BuyUsdRequest {
  amountArs: number;
}

export interface BuyUsdResponse {
  success: boolean;
  message: string;
  amountArs: number;
  amountUsd: number;
  exchangeRate: number;
  taxAmount: number;
  taxPercentage: number;
  totalDebitado: number;
  newBalanceArs: number;
  newBalanceUsd: number;
}

export interface SellUsdRequest {
  amountUsd: number;
}

export interface SellUsdResponse {
  success: boolean;
  message: string;
  amountUsd: number;
  amountArs: number;
  exchangeRate: number;
  newBalanceUsd: number;
  newBalanceArs: number;
}

export interface UserAccount {
  id: string;
  balance: number;
  alias: string;
  cvu: string;
  currency: 'ARS' | 'USD';
}

export interface UserAccountsResponse {
  success: boolean;
  accounts: UserAccount[];
}

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private baseUrl = environment.apiUrl;
  
  // Subject para notificar cuando se crea una cuenta USD
  private accountCreatedSubject = new BehaviorSubject<boolean>(false);
  public accountCreated$ = this.accountCreatedSubject.asObservable();

  constructor(private http: HttpClient) {}

  /**
   * Abre una cuenta en dólares para el usuario autenticado
   */
  openUsdAccount(): Observable<any> {
    return this.http.post(`${this.baseUrl}/accounts/usd`, {})
      .pipe(
        tap((response: any) => {
          if (response.success) {
            this.accountCreatedSubject.next(true);
          }
        })
      );
  }

  /**
   * Compra dólares desde una cuenta ARS a una cuenta USD
   */
  buyUsd(accountArsId: string, accountUsdId: string, amountArs: number): Observable<BuyUsdResponse> {
    const request: BuyUsdRequest = { amountArs };
    return this.http.post<BuyUsdResponse>(
      `${this.baseUrl}/accounts/${accountArsId}/buy-usd/${accountUsdId}`,
      request
    );
  }

  /**
   * Vende dólares desde una cuenta USD a una cuenta ARS
   */
  sellUsd(accountUsdId: string, accountArsId: string, amountUsd: number): Observable<SellUsdResponse> {
    const request: SellUsdRequest = { amountUsd };
    return this.http.post<SellUsdResponse>(
      `${this.baseUrl}/accounts/${accountUsdId}/sell-usd/${accountArsId}`,
      request
    );
  }

  /**
   * Obtiene todas las cuentas del usuario (tanto ARS como USD)
   */
  getUserAccounts(): Observable<UserAccount[]> {
    return this.http.get<UserAccountsResponse>(`${this.baseUrl}/accounts/user-accounts`)
      .pipe(
        map(response => {
          if (!response.success) {
            throw new Error('Error al obtener las cuentas');
          }
          return response.accounts;
        })
      );
  }

  /**
   * Resetea el estado de cuenta creada
   */
  resetAccountCreatedState(): void {
    this.accountCreatedSubject.next(false);
  }
}
