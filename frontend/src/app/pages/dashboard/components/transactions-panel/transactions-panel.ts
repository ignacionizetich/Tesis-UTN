import {
  Component,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { TransactionService } from '../../../../services/transaction/transaction.service';
import { ModalService } from '../../../../services/modal/modal.service';
import { ToastService } from '../../../../services/toast/toast.service';
import { formatMoney as formatMoneyShared } from '../../../../shared/utils/money-format';
import {
  formatDateTime,
  formatDateTimeDetailed,
} from '../../../../shared/utils/date-format';
import Transaction from '../../../../models/transaction';
import { logger } from '../../../../shared/utils/logger';
import { ReceiptPdfService } from '../../../../services/receipt-pdf/receipt-pdf.service';

@Component({
  selector: 'app-transactions-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './transactions-panel.html',
  styleUrls: ['./transactions-panel.css'],
})
export class TransactionsPanelComponent implements OnInit, OnChanges, OnDestroy {
  /** Cuenta activa según el toggle ARS/USD del dashboard. */
  @Input() accountId: string | null = null;
  @Input() currency: 'ARS' | 'USD' = 'ARS';

  currentModal: string | null = null;
  recentTransactions: Transaction[] = [];
  allTransactions: Transaction[] = [];
  displayedTransactions: Transaction[] = [];
  selectedTransaction: Transaction | null = null;
  receiptBusy = false;

  transactionPageSize = 20;
  currentTransactionPage = 0;
  isLoadingMoreTransactions = false;
  hasLoadedAllTransactions = false;
  hasMoreTransactions = false;
  isLoadingList = false;

  private isInAllTransactionsModal = false;
  private forceKeepTransactions = false;
  private detailOpenedFromAll = false;
  private hiddenTransactionIds = new Set<number>();
  private subscriptions: Subscription[] = [];
  private lastLoadedAccountId: string | null = null;

  constructor(
    private transactionService: TransactionService,
    private modalService: ModalService,
    private toast: ToastService,
    private receiptPdf: ReceiptPdfService
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

    void this.reloadForAccount(false);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['accountId'] && !changes['accountId'].firstChange) {
      void this.reloadForAccount(true);
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((s) => s.unsubscribe());
  }

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

  amountLabel(tx: Transaction): string {
    const sign = tx.type === 'income' ? '+' : '-';
    const code = tx.currency || this.currency;
    return `${sign}$${this.formatAmount(tx.amount)} ${code}`;
  }

  initials(tx: Transaction): string {
    if (tx.kind === 'loan_credit') return 'PR';
    if (tx.kind === 'loan_payment') return 'CU';
    const source =
      tx.kind === 'transfer'
        ? tx.counterpartyName || tx.description
        : tx.kind === 'buy_usd'
          ? 'CD'
          : tx.kind === 'sell_usd'
            ? 'VD'
            : '?';
    const parts = source.trim().split(/\s+/).filter(Boolean);
    if (parts.length >= 2) {
      return `${parts[0].charAt(0)}${parts[1].charAt(0)}`.toUpperCase();
    }
    return source.slice(0, 2).toUpperCase();
  }

  iconClass(tx: Transaction): string {
    if (tx.kind === 'buy_usd') return 'buy';
    if (tx.kind === 'sell_usd') return 'sell';
    if (tx.kind === 'loan_credit') return 'loan-in';
    if (tx.kind === 'loan_payment') return 'loan-out';
    return tx.type;
  }

  getTransactionOrigin(transaction: Transaction): string {
    if (transaction.kind === 'loan_credit') return 'Arcash Préstamos';
    if (transaction.kind === 'loan_payment') return 'Tu cuenta';
    if (transaction.kind === 'buy_usd' || transaction.kind === 'sell_usd') {
      return transaction.from || 'Tu cuenta';
    }
    if (transaction.type === 'income') {
      return transaction.counterpartyName || transaction.from || 'Cuenta externa';
    }
    return 'Tu cuenta';
  }

  getTransactionDestination(transaction: Transaction): string {
    if (transaction.kind === 'loan_credit') return 'Tu cuenta';
    if (transaction.kind === 'loan_payment') return 'Arcash Préstamos';
    if (transaction.kind === 'buy_usd' || transaction.kind === 'sell_usd') {
      return transaction.to || 'Tu cuenta';
    }
    if (transaction.type === 'income') {
      return 'Tu cuenta';
    }
    return transaction.counterpartyName || transaction.to || 'Cuenta externa';
  }

  detailTitle(tx: Transaction): string {
    if (tx.kind === 'buy_usd') return 'Detalle de compra';
    if (tx.kind === 'sell_usd') return 'Detalle de venta';
    if (tx.kind === 'loan_credit') return 'Detalle del préstamo';
    if (tx.kind === 'loan_payment') return 'Detalle de cuota';
    return 'Detalle de transferencia';
  }

  async downloadReceipt(): Promise<void> {
    if (!this.selectedTransaction || this.receiptBusy) return;
    this.receiptBusy = true;
    try {
      await this.receiptPdf.download(this.selectedTransaction, this.receiptLabels(this.selectedTransaction));
      this.toast.show('Comprobante descargado', 'success');
    } catch (error) {
      logger.error('Error generando PDF', error);
      this.toast.show('No se pudo generar el comprobante', 'error');
    } finally {
      this.receiptBusy = false;
    }
  }

  async shareReceipt(): Promise<void> {
    if (!this.selectedTransaction || this.receiptBusy) return;
    this.receiptBusy = true;
    try {
      const result = await this.receiptPdf.share(
        this.selectedTransaction,
        this.receiptLabels(this.selectedTransaction)
      );
      if (result === 'shared') {
        this.toast.show('Comprobante listo para compartir', 'success');
      } else {
        this.toast.show('Comprobante descargado para que lo compartas', 'success');
      }
    } catch (error: unknown) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return;
      }
      logger.error('Error compartiendo PDF', error);
      this.toast.show('No se pudo compartir el comprobante', 'error');
    } finally {
      this.receiptBusy = false;
    }
  }

  private receiptLabels(tx: Transaction) {
    return {
      title: this.detailTitle(tx),
      description: tx.description,
      amountLabel: this.amountLabel(tx),
      origin: this.getTransactionOrigin(tx),
      destination: this.getTransactionDestination(tx),
    };
  }

  onModalBackdropClick(event: MouseEvent, modalType: string): void {
    const target = event.target as HTMLElement;
    if (
      target.classList.contains('tx-modal') ||
      target.classList.contains('tx-all-modal')
    ) {
      if (modalType === 'transaction') {
        this.closeTransactionModal();
      } else if (modalType === 'allTransactions') {
        this.closeAllTransactionsModal();
      }
    }
  }

  openTransactionModal(transaction: Transaction): void {
    this.detailOpenedFromAll =
      this.currentModal === 'allTransactions' || this.isInAllTransactionsModal;
    this.selectedTransaction = transaction;
    this.modalService.openModal('transaction');
  }

  closeTransactionModal(): void {
    this.selectedTransaction = null;
    if (this.detailOpenedFromAll) {
      this.detailOpenedFromAll = false;
      this.isInAllTransactionsModal = true;
      this.forceKeepTransactions = true;
      this.modalService.openModal('allTransactions');
      return;
    }
    this.modalService.closeModal();
  }

  async openAllTransactionsModal(): Promise<void> {
    try {
      this.isInAllTransactionsModal = true;
      this.forceKeepTransactions = true;
      this.currentTransactionPage = 0;
      this.hasLoadedAllTransactions = false;
      this.isLoadingMoreTransactions = false;
      this.hasMoreTransactions = true;

      await this.transactionService.loadAllTransactions(false, this.accountId);
      this.updateDisplayedTransactions();
      this.modalService.openModal('allTransactions');
    } catch (error) {
      logger.error('Error cargando todas las transacciones:', error);
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
      logger.error('Error cargando más transacciones:', error);
      this.toast.show('Error al cargar más transacciones', 'error');
      this.currentTransactionPage--;
    } finally {
      this.isLoadingMoreTransactions = false;
    }
  }

  closeAllTransactionsModal(): void {
    this.isInAllTransactionsModal = false;
    this.forceKeepTransactions = false;
    this.detailOpenedFromAll = false;
    this.modalService.closeModal();
    this.updateDisplayedTransactions();
  }

  private async reloadForAccount(force: boolean): Promise<void> {
    if (!this.accountId) {
      this.recentTransactions = [];
      this.allTransactions = [];
      return;
    }
    if (!force && this.lastLoadedAccountId === this.accountId) {
      return;
    }

    this.isLoadingList = true;
    try {
      await this.transactionService.loadAllTransactions(force, this.accountId);
      this.lastLoadedAccountId = this.accountId;
    } catch {
      // toast ya no siempre: el dashboard también carga
    } finally {
      this.isLoadingList = false;
    }
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
