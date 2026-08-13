import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import Transaction from '../../models/transaction';
import { TransactionHistoryStore } from '../transaction-history-store/transaction-history.store';
import { CacheService } from '../cache-service/cache.service';
import { CacheConfig } from '../../models/cache.interface';
import { PaginationConfig } from '../../models/common.interface';
import { formatCurrencyArs } from '../../shared/utils/money-format';
import { formatDateTime, formatDateTimeDetailed } from '../../shared/utils/date-format';

/**
 * Capa de UI sobre TransactionHistoryStore:
 * - localStorage cache (TTL)
 * - recent / displayed streams
 * - paginación client-side
 * - helpers de formato para templates
 *
 * HTTP y mapeo de DTOs viven solo en TransactionHistoryStore.
 * Las páginas deben inyectar TransactionService (no el store),
 * salvo casos de invalidación/clear de sesión.
 */
@Injectable({
    providedIn: 'root'
})
export class TransactionService {
  private readonly cacheConfig: CacheConfig = {
    key: 'arcash_transactions_cache',
    expiryKey: 'arcash_transactions_cache_expiry',
    duration: 5 * 60 * 1000 // 5 minutos
  };

  private recentTransactionsSubject = new BehaviorSubject<Transaction[]>([]);
  public recentTransactions$ = this.recentTransactionsSubject.asObservable();

  private allTransactionsSubject = new BehaviorSubject<Transaction[]>([]);
  public allTransactions$ = this.allTransactionsSubject.asObservable();

  // Paginación optimizada
  private pagination: PaginationConfig = {
    page: 0,
    size: 20,
    totalPages: 0,
    hasMore: false
  };
  
  private displayedTransactionsSubject = new BehaviorSubject<Transaction[]>([]);
  public displayedTransactions$ = this.displayedTransactionsSubject.asObservable();

  constructor(
    private transactionHistoryStore: TransactionHistoryStore,
    private cacheService: CacheService
  ) {
    this.initializeSubscriptions();
  }

  private initializeSubscriptions(): void {
    this.transactionHistoryStore.transactions$.subscribe(transactions => {
      this.allTransactionsSubject.next(transactions);
      this.recentTransactionsSubject.next(transactions.slice(0, 3));
      this.resetPagination();
    });
  }

  async loadAllTransactions(forceReload: boolean = false): Promise<void> {
    try {
      if (!forceReload) {
        const cachedTransactions = this.cacheService.getCache<Transaction[]>(this.cacheConfig);
        if (cachedTransactions) {
          this.allTransactionsSubject.next(cachedTransactions);
          this.recentTransactionsSubject.next(cachedTransactions.slice(0, 3));
          this.resetPagination();
          return;
        }
      }
      await this.transactionHistoryStore.load();
      
      const transactions = this.allTransactionsSubject.value;
      if (transactions.length > 0) {
        this.cacheService.setCache(this.cacheConfig, transactions);
      }
    } catch (error) {
      console.error('Error cargando transacciones:', error);
      throw error;
    }
  }

  // Métodos de acceso a datos
  getRecentTransactions(): Transaction[] {
    return this.recentTransactionsSubject.value;
  }

  getAllTransactions(): Transaction[] {
    return this.allTransactionsSubject.value;
  }

  getDisplayedTransactions(): Transaction[] {
    return this.displayedTransactionsSubject.value;
  }

  // Paginación optimizada
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
    this.pagination.totalPages = Math.ceil(allTransactions.length / this.pagination.size);
    this.pagination.hasMore = this.pagination.page < this.pagination.totalPages - 1;
  }

  private updateDisplayedTransactions(): void {
    const allTransactions = this.allTransactionsSubject.value;
    const startIndex = this.pagination.page * this.pagination.size;
    const endIndex = startIndex + this.pagination.size;
    this.displayedTransactionsSubject.next(allTransactions.slice(startIndex, endIndex));
  }

  // Métodos de formateo (delegados a shared utils)
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

  // Cache management
  invalidateCache(): void {
    this.cacheService.clearCache(this.cacheConfig);
  }
}
