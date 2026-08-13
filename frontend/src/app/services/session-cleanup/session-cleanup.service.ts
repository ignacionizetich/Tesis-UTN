import { Injectable } from '@angular/core';
import { SessionStore } from '../../core/session/session.store';
import { UserDataStore } from '../user-data-store/user-data.store';
import { TransactionHistoryStore } from '../transaction-history-store/transaction-history.store';
import { FavoriteService } from '../favorite/favorite.service';
import { TransactionService } from '../transaction/transaction.service';
import { CacheService } from '../cache/cache.service';

/**
 * Limpieza unificada de sesión + caches en memoria y localStorage.
 */
@Injectable({
  providedIn: 'root',
})
export class SessionCleanupService {
  constructor(
    private sessionStore: SessionStore,
    private userDataStore: UserDataStore,
    private transactionHistoryStore: TransactionHistoryStore,
    private favoriteService: FavoriteService,
    private transactionService: TransactionService,
    private cacheService: CacheService
  ) {}

  clearAll(): void {
    this.sessionStore.clear();
    this.userDataStore.clear();
    this.transactionHistoryStore.clear();
    try {
      this.favoriteService.invalidateCache();
      this.transactionService.invalidateCache();
    } catch (error) {
      console.error('Error invalidando caches de dominio:', error);
    }
    this.cacheService.clearAllArCashCaches();
  }
}
