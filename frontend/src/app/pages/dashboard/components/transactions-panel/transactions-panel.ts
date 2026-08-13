import {
  Component,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { TransactionService } from '../../../../services/transaction-service/transaction.service';
import { ModalService } from '../../../../services/modal-service/modal.service';
import { ToastService } from '../../../../services/toast-service/toast.service';
import { formatMoney as formatMoneyShared } from '../../../../shared/utils/money-format';
import {
  formatDateTime,
  formatDateTimeDetailed,
} from '../../../../shared/utils/date-format';
import Transaction from '../../../../models/transaction';

@Component({
  selector: 'app-transactions-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './transactions-panel.html',
  styleUrls: ['../../styles/modals-shared.css', '../../styles/transactions.css'],
})
export class TransactionsPanelComponent implements OnInit, OnDestroy {
  currentModal: string | null = null;
  recentTransactions: Transaction[] = [];
  allTransactions: Transaction[] = [];
  displayedTransactions: Transaction[] = [];
  selectedTransaction: Transaction | null = null;

  transactionPageSize = 20;
  currentTransactionPage = 0;
  isLoadingMoreTransactions = false;
  hasLoadedAllTransactions = false;
  hasMoreTransactions = false;

  private isInAllTransactionsModal = false;
  private forceKeepTransactions = false;
  private hiddenTransactionIds = new Set<number>();
  private subscriptions: Subscription[] = [];

  constructor(
    private transactionService: TransactionService,
    private modalService: ModalService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.subscriptions.push(
      this.modalService.modalState$.subscribe((state) => {
        this.currentModal = state.currentModal;
        this.isInAllTransactionsModal = state.currentModal === 'allTransactions';
      }),
      this.transactionService.recentTransactions$.subscribe((transactions) => {
        this.recentTransactions = transactions.filter(
          (t) => !this.hiddenTransactionIds.has(t.id)
        );
      }),
      this.transactionService.allTransactions$.subscribe((transactions) => {
        this.allTransactions = transactions;
        if (!this.isInAllTransactionsModal || !this.forceKeepTransactions) {
          this.updateDisplayedTransactions();
        }
      }),
      this.transactionService.displayedTransactions$.subscribe((transactions) => {
        if (this.isInAllTransactionsModal) {
          this.displayedTransactions = transactions.filter(
            (t) => !this.hiddenTransactionIds.has(t.id)
          );
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((s) => s.unsubscribe());
  }

  /** Usado por el polling del dashboard para no pisar la lista paginada. */
  get isViewingAllTransactions(): boolean {
    return this.isInAllTransactionsModal;
  }

  trackTransaction(_index: number, transaction: Transaction): number {
    return transaction.id;
  }

  formatAmount(amount: number): string {
    return formatMoneyShared(amount);
  }

  formatDate(date: Date): string {
    return formatDateTime(date);
  }

  formatDateDetailed(date: Date): string {
    return formatDateTimeDetailed(date);
  }

  getTransactionOrigin(transaction: Transaction): string {
    if (transaction.type === 'income') {
      return transaction.from || 'Cuenta externa';
    }
    return 'Tu cuenta';
  }

  getTransactionDestination(transaction: Transaction): string {
    if (transaction.type === 'income') {
      return 'Tu cuenta';
    }
    return transaction.to || 'Cuenta externa';
  }

  onModalBackdropClick(event: MouseEvent, modalType: string): void {
    if ((event.target as HTMLElement).classList.contains('modal')) {
      if (modalType === 'transaction') {
        this.closeTransactionModal();
      } else if (modalType === 'allTransactions') {
        this.closeAllTransactionsModal();
      }
    }
  }

  openTransactionModal(transaction: Transaction): void {
    this.selectedTransaction = transaction;
    this.modalService.openModal('transaction');
  }

  closeTransactionModal(): void {
    this.modalService.closeModal();
    this.selectedTransaction = null;
  }

  async openAllTransactionsModal(): Promise<void> {
    try {
      this.isInAllTransactionsModal = true;
      this.forceKeepTransactions = true;
      this.currentTransactionPage = 0;
      this.hasLoadedAllTransactions = false;
      this.isLoadingMoreTransactions = false;
      this.hasMoreTransactions = true;

      await this.transactionService.loadAllTransactions(false);
      this.updateDisplayedTransactions();
      this.modalService.openModal('allTransactions');
    } catch (error) {
      console.error('Error cargando todas las transacciones:', error);
      this.toast.show('Error al cargar las transacciones', 'error');
    }
  }

  async loadMoreTransactions(): Promise<void> {
    if (this.isLoadingMoreTransactions || !this.hasMoreTransactions) {
      return;
    }

    this.isLoadingMoreTransactions = true;
    this.forceKeepTransactions = true;

    try {
      this.currentTransactionPage++;
      this.transactionService.loadMoreTransactions();
      this.updateDisplayedTransactionsFromService();
    } catch (error) {
      console.error('Error cargando más transacciones:', error);
      this.toast.show('Error al cargar más transacciones', 'error');
      this.currentTransactionPage--;
    } finally {
      this.isLoadingMoreTransactions = false;
    }
  }

  closeAllTransactionsModal(): void {
    this.isInAllTransactionsModal = false;
    this.forceKeepTransactions = false;
    this.modalService.closeModal();
    this.updateDisplayedTransactions();
  }

  private updateDisplayedTransactions(): void {
    if (this.isInAllTransactionsModal) {
      this.updateDisplayedTransactionsFromService();
      return;
    }

    const filteredTransactions = this.allTransactions.filter(
      (transaction) => !this.hiddenTransactionIds.has(transaction.id)
    );

    this.displayedTransactions = filteredTransactions.slice(
      0,
      (this.currentTransactionPage + 1) * this.transactionPageSize
    );

    const totalFilteredTransactions = filteredTransactions.length;
    const currentlyDisplayed = this.displayedTransactions.length;

    this.hasMoreTransactions = currentlyDisplayed < totalFilteredTransactions;
    this.hasLoadedAllTransactions = !this.hasMoreTransactions;
  }

  private updateDisplayedTransactionsFromService(): void {
    const displayed = this.transactionService.getDisplayedTransactions();
    this.displayedTransactions = displayed.filter(
      (transaction) => !this.hiddenTransactionIds.has(transaction.id)
    );

    this.hasMoreTransactions = this.transactionService.hasMoreTransactions();
    this.hasLoadedAllTransactions = !this.hasMoreTransactions;
  }
}
