import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import Transaction from '../../models/transaction';
import { TransactionHistoryStore } from '../transaction-history-store/transaction-history.store';
import { CacheService } from '../cache/cache.service';
import { CacheConfig } from '../../models/cache.interface';
import { PaginationConfig } from '../../models/common.interface';
import { formatCurrencyArs } from '../../shared/utils/money-format';
import { formatDateTime, formatDateTimeDetailed } from '../../shared/utils/date-format';
import { logger } from '../../shared/utils/logger';

/**
 * Capa de UI sobre TransactionHistoryStore:
 * - localStorage cache (TTL) por cuenta
 * - recent / displayed streams
 * - paginación client-side
 */
@Injectable({
  providedIn: 'root',
})
export class TransactionService {
  private readonly cacheDuration = 5 * 60 * 1000;

  private recentTransactionsSubject = new BehaviorSubject<Transaction[]>([]);
  public recentTransactions$ = this.recentTransactionsSubject.asObservable();

  private allTransactionsSubject = new BehaviorSubject<Transaction[]>([]);
  public allTransactions$ = this.allTransactionsSubject.asObservable();

  private pagination: PaginationConfig = {
    page: 0,
    size: 20,
    totalPages: 0,
    hasMore: false,
  };

  private displayedTransactionsSubject = new BehaviorSubject<Transaction[]>([]);
  public displayedTransactions$ = this.displayedTransactionsSubject.asObservable();

  private activeAccountId: string | null = null;

  constructor(
    private transactionHistoryStore: TransactionHistoryStore,
    private cacheService: CacheService
  ) {
    this.initializeSubscriptions();
  }

  private initializeSubscriptions(): void {
    this.transactionHistoryStore.transactions$.subscribe((transactions) => {
      this.allTransactionsSubject.next(transactions);
      this.recentTransactionsSubject.next(transactions.slice(0, 5));
      this.resetPagination();
    });
  }

  private cacheConfigFor(accountId: string): CacheConfig {
    return {
      key: `arcash_transactions_cache_${accountId}`,
      expiryKey: `arcash_transactions_cache_expiry_${accountId}`,
      duration: this.cacheDuration,
    };
  }

  async loadAllTransactions(
    forceReload: boolean = false,
    accountId?: string | null
  ): Promise<void> {
    const resolvedId = accountId || this.activeAccountId;
    if (!resolvedId) {
      this.allTransactionsSubject.next([]);
      this.recentTransactionsSubject.next([]);
      this.resetPagination();
      return;
    }

    this.activeAccountId = resolvedId;

    try {
      if (!forceReload) {
        const cached = this.cacheService.getCache<Transaction[]>(
          this.cacheConfigFor(resolvedId)
        );
        if (cached) {
          this.allTransactionsSubject.next(cached);
          this.recentTransactionsSubject.next(cached.slice(0, 5));
          this.resetPagination();
          return;
        }
      }

      await this.transactionHistoryStore.load(resolvedId);

      const transactions = this.allTransactionsSubject.value;
      if (transactions.length > 0) {
        this.cacheService.setCache(this.cacheConfigFor(resolvedId), transactions);
      }
    } catch (error) {
      logger.error('Error cargando transacciones:', error);
      throw error;
    }
  }

  getRecentTransactions(): Transaction[] {
    return this.recentTransactionsSubject.value;
  }

  getAllTransactions(): Transaction[] {
    return this.allTransactionsSubject.value;
  }

  getDisplayedTransactions(): Transaction[] {
    return this.displayedTransactionsSubject.value;
  }

  resetPagination(): void {
    this.pagination.page = 0;
    this.updatePagination();
    this.updateDisplayedTransactions();
  }

  loadMoreTransactions(): void {
    const allTransactions = this.allTransactionsSubject.value;
    this.updatePagination();

    if (this.pagination.hasMore) {
      this.pagination.page++;
      const startIndex = this.pagination.page * this.pagination.size;
      const endIndex = startIndex + this.pagination.size;
      const newTransactions = allTransactions.slice(startIndex, endIndex);

      const currentDisplayed = this.displayedTransactionsSubject.value;
      this.displayedTransactionsSubject.next([...currentDisplayed, ...newTransactions]);
      this.updatePagination();
    }
  }

  hasMoreTransactions(): boolean {
    return this.pagination.hasMore;
  }

  private updatePagination(): void {
    const allTransactions = this.allTransactionsSubject.value;
    this.pagination.totalPages = Math.ceil(
      allTransactions.length / this.pagination.size
    );
    this.pagination.hasMore = this.pagination.page < this.pagination.totalPages - 1;
  }

  private updateDisplayedTransactions(): void {
    const allTransactions = this.allTransactionsSubject.value;
    const startIndex = this.pagination.page * this.pagination.size;
    const endIndex = startIndex + this.pagination.size;
    this.displayedTransactionsSubject.next(
      allTransactions.slice(startIndex, endIndex)
    );
  }

  formatAmount(amount: number): string {
    return formatCurrencyArs(amount);
  }

  formatDate(date: Date): string {
    return formatDateTime(date);
  }

  formatDateDetailed(date: Date): string {
    return formatDateTimeDetailed(date);
  }

  getTransactionClass(transaction: Transaction): string {
    if (transaction.status === 'FAILED') return 'monto fallida';
    return transaction.type === 'income' ? 'monto positivo' : 'monto negativo';
  }

  invalidateCache(accountId?: string | null): void {
    const id = accountId || this.activeAccountId;
    if (id) {
      this.cacheService.clearCache(this.cacheConfigFor(id));
    }
  }
}
