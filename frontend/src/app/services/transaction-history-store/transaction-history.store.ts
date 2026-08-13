import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, lastValueFrom } from 'rxjs';
import Transaction, { TransactionKind } from '../../models/transaction';
import { environment } from '../../../environments/environment';
import { SessionStore } from '../../core/session/session.store';
import { formatMoney } from '../../shared/utils/money-format';
import { logger } from '../../shared/utils/logger';

interface TransactionApiDto {
  idTransaction: number;
  idOperation?: string;
  idOrigin: number;
  idDestination: number;
  amount: number;
  state?: string;
  date?: string;
  originUsername?: string;
  destinationUsername?: string;
  originFullName?: string | null;
  destinationFullName?: string | null;
  originUserId?: number;
  destinationUserId?: number;
  sameOwner?: boolean;
  originAlias?: string;
  destinationAlias?: string;
  currency?: string;
  originalAmount?: number;
  originalCurrency?: string;
  exchangeRate?: number;
  converted?: boolean;
  taxAmount?: number;
  taxPercentage?: number;
  operationType?: string;
  notes?: string;
}

/**
 * Fuente HTTP del historial: GET + mapeo a modelo Transaction + memoria (transactions$).
 *
 * No cachea en localStorage ni pagina — eso es TransactionService.
 */
@Injectable({
  providedIn: 'root',
})
export class TransactionHistoryStore {
  private readonly baseUrl = environment.apiUrl;

  private readonly transactionsSubject = new BehaviorSubject<Transaction[]>([]);
  readonly transactions$ = this.transactionsSubject.asObservable();

  private lastLoadedAccountId: string | null = null;

  constructor(
    private http: HttpClient,
    private sessionStore: SessionStore
  ) {}

  clear(): void {
    this.transactionsSubject.next([]);
    this.lastLoadedAccountId = null;
  }

  getCurrent(): Transaction[] {
    return this.transactionsSubject.value;
  }

  getLastLoadedAccountId(): string | null {
    return this.lastLoadedAccountId;
  }

  async load(accountId?: string | null): Promise<Transaction[]> {
    const resolvedId = accountId || this.sessionStore.getAccountId();
    if (!resolvedId) {
      this.transactionsSubject.next([]);
      this.lastLoadedAccountId = null;
      return [];
    }

    try {
      const response = await lastValueFrom(
        this.http.get<TransactionApiDto[]>(
          `${this.baseUrl}/transactions/${resolvedId}/getTransactions`
        )
      );

      if (!response || !Array.isArray(response)) {
        this.transactionsSubject.next([]);
        this.lastLoadedAccountId = resolvedId;
        return [];
      }

      const viewingAccountId = parseInt(resolvedId, 10);
      const transactions = response.map((tx) =>
        this.mapTransaction(tx, viewingAccountId)
      );

      this.transactionsSubject.next(transactions);
      this.lastLoadedAccountId = resolvedId;
      return transactions;
    } catch (error) {
      logger.error('Error en TransactionHistoryStore.load:', error);
      this.transactionsSubject.next([]);
      throw error;
    }
  }

  private mapTransaction(tx: TransactionApiDto, viewingAccountId: number): Transaction {
    const sameOwner =
      tx.sameOwner === true ||
      (tx.originUserId != null &&
        tx.destinationUserId != null &&
        tx.originUserId === tx.destinationUserId);
    const converted = !!tx.converted;
    const originalCurrency = (tx.originalCurrency || '').toUpperCase();
    const currency = (tx.currency || '').toUpperCase();
    const operationType = (tx.operationType || '').toUpperCase();

    const isBuyUsd =
      converted && sameOwner && originalCurrency === 'ARS' && currency === 'USD';
    const isSellUsd =
      converted && sameOwner && originalCurrency === 'USD' && currency === 'ARS';

    const isIncoming = tx.idDestination === viewingAccountId;
    const amountBase = parseFloat(String(tx.amount)) || 0;
    const originalAmount = parseFloat(String(tx.originalAmount ?? 0)) || 0;
    const taxAmount = parseFloat(String(tx.taxAmount ?? 0)) || 0;
    const exchangeRate = tx.exchangeRate != null ? Number(tx.exchangeRate) : undefined;

    let kind: TransactionKind = 'transfer';
    let type: 'income' | 'expense' = isIncoming ? 'income' : 'expense';
    let description: string;
    let subtitle: string | undefined;
    let displayAmount = amountBase;
    let displayCurrency: 'ARS' | 'USD' =
      currency === 'USD' ? 'USD' : 'ARS';
    let counterpartyName: string | undefined;
    let from = tx.originFullName || tx.originAlias || tx.originUsername;
    let to = tx.destinationFullName || tx.destinationAlias || tx.destinationUsername;

    if (operationType === 'LOAN_CREDIT') {
      kind = 'loan_credit';
      type = 'income';
      description = 'Préstamo acreditado';
      subtitle = tx.notes || 'Ingreso por préstamo Arcash';
      displayCurrency = 'ARS';
      from = 'Arcash Préstamos';
      to = 'Tu cuenta';
    } else if (operationType === 'LOAN_PAYMENT') {
      kind = 'loan_payment';
      type = 'expense';
      description = 'Pago de cuota';
      subtitle = tx.notes || 'Cuota de préstamo';
      displayCurrency = 'ARS';
      from = 'Tu cuenta';
      to = 'Arcash Préstamos';
    } else if (isBuyUsd) {
      kind = 'buy_usd';
      description = 'Compraste dólares';
      if (viewingAccountId === tx.idOrigin) {
        type = 'expense';
        displayAmount = originalAmount + taxAmount;
        displayCurrency = 'ARS';
        subtitle = exchangeRate
          ? `≈ ${formatMoney(amountBase)} USD · cotiz. $${formatMoney(exchangeRate)}`
          : `≈ ${formatMoney(amountBase)} USD`;
      } else {
        type = 'income';
        displayAmount = amountBase;
        displayCurrency = 'USD';
        subtitle = exchangeRate
          ? `Desde ARS · cotiz. $${formatMoney(exchangeRate)}`
          : 'Desde tu cuenta en pesos';
      }
    } else if (isSellUsd) {
      kind = 'sell_usd';
      description = 'Vendiste dólares';
      if (viewingAccountId === tx.idOrigin) {
        type = 'expense';
        displayAmount = originalAmount + taxAmount;
        displayCurrency = 'USD';
        subtitle = exchangeRate
          ? `≈ ${formatMoney(amountBase)} ARS · cotiz. $${formatMoney(exchangeRate)}`
          : `≈ ${formatMoney(amountBase)} ARS`;
      } else {
        type = 'income';
        displayAmount = amountBase;
        displayCurrency = 'ARS';
        subtitle = exchangeRate
          ? `Desde USD · cotiz. $${formatMoney(exchangeRate)}`
          : 'Desde tu cuenta en dólares';
      }
    } else {
      counterpartyName = isIncoming
        ? tx.originFullName || tx.originUsername || tx.originAlias || 'Desconocido'
        : tx.destinationFullName ||
          tx.destinationUsername ||
          tx.destinationAlias ||
          'Desconocido';
      description = isIncoming
        ? `Recibiste de ${counterpartyName}`
        : `Enviaste a ${counterpartyName}`;
      const otherAlias = isIncoming ? tx.originAlias : tx.destinationAlias;
      if (otherAlias && otherAlias !== counterpartyName) {
        subtitle = otherAlias;
      }
      displayCurrency = currency === 'USD' ? 'USD' : 'ARS';
    }

    return {
      id: tx.idTransaction,
      type,
      kind,
      description,
      subtitle,
      amount: displayAmount,
      date: tx.date ? new Date(tx.date) : new Date(),
      from,
      to,
      counterpartyName,
      originId: tx.idOrigin,
      destinationId: tx.idDestination,
      status: (tx.state as Transaction['status']) || 'COMPLETED',
      currency: displayCurrency,
      exchangeRate,
      originalAmount: originalAmount || undefined,
      originalCurrency:
        originalCurrency === 'USD' || originalCurrency === 'ARS'
          ? originalCurrency
          : undefined,
      taxAmount: taxAmount || undefined,
      taxPercentage: tx.taxPercentage != null ? Number(tx.taxPercentage) : undefined,
      converted,
      sameOwner,
      idOperation: tx.idOperation,
      operationType: tx.operationType,
      notes: tx.notes,
    };
  }
}
