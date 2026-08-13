import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, lastValueFrom } from 'rxjs';
import Transaction from '../../models/transaction';
import { environment } from '../../../environments/environment';
import { SessionStore } from '../../core/session/session-store';

/**
 * Fuente HTTP del historial: GET + mapeo a modelo Transaction + memoria (transactions$).
 *
 * No cachea en localStorage ni pagina — eso es TransactionService.
 * Usar desde SessionCleanup / Auth; la UI consume TransactionService.
 */
@Injectable({
  providedIn: 'root',
})
export class TransactionHistoryStore {
  private readonly baseUrl = environment.apiUrl;

  private readonly transactionsSubject = new BehaviorSubject<Transaction[]>([]);
  readonly transactions$ = this.transactionsSubject.asObservable();

  constructor(
    private http: HttpClient,
    private sessionStore: SessionStore
  ) {}

  clear(): void {
    this.transactionsSubject.next([]);
  }

  getCurrent(): Transaction[] {
    return this.transactionsSubject.value;
  }

  async load(): Promise<Transaction[]> {
    const accountId = this.sessionStore.getAccountId();
    if (!accountId) {
      this.transactionsSubject.next([]);
      return [];
    }

    try {
      const response = await lastValueFrom(
        this.http.get<any[]>(`${this.baseUrl}/transactions/${accountId}/getTransactions`)
      );

      if (!response || !Array.isArray(response)) {
        this.transactionsSubject.next([]);
        return [];
      }

      const currentAccountIdNum = parseInt(accountId, 10);
      const transactions: Transaction[] = response.map((tx: any) => {
        const isIncoming = tx.idOrigin !== currentAccountIdNum;

        return {
          id: tx.idTransaction,
          type: isIncoming ? 'income' : 'expense',
          description: isIncoming
            ? `Transferencia de ${tx.originAlias || tx.originUsername || 'Desconocido'}`
            : `Transferencia a ${tx.destinationAlias || tx.destinationUsername || 'Desconocido'}`,
          amount: parseFloat(tx.amount) || 0,
          date: tx.date ? new Date(tx.date) : new Date(),
          from: tx.originAlias || tx.originUsername,
          to: tx.destinationAlias || tx.destinationUsername,
          originId: tx.idOrigin,
          destinationId: tx.idDestination,
          status: tx.state || 'COMPLETED',
        };
      });

      this.transactionsSubject.next(transactions);
      return transactions;
    } catch (error) {
      console.error('Error en TransactionHistoryStore.load:', error);
      this.transactionsSubject.next([]);
      throw error;
    }
  }
}
