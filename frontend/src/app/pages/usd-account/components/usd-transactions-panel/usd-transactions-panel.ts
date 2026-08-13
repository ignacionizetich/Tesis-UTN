import { Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import Transaction from '../../../../models/transaction';
import { TransactionService } from '../../../../services/transaction/transaction.service';
import { ToastService } from '../../../../services/toast/toast.service';
import { ReceiptPdfService } from '../../../../services/receipt-pdf/receipt-pdf.service';
import { formatMoney } from '../../../../shared/utils/money-format';
import { formatDateTime } from '../../../../shared/utils/date-format';
import { logger } from '../../../../shared/utils/logger';

@Component({
  selector: 'app-usd-transactions-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './usd-transactions-panel.html',
  styleUrls: ['../../usd-account.css', '../../styles/transaction-detail.css'],
})
export class UsdTransactionsPanelComponent implements OnInit, OnChanges, OnDestroy {
  /** Si hay cuenta USD, el panel carga/filtra movimientos. */
  @Input() enabled = false;

  usdTransactions: Transaction[] = [];
  filteredTransactions: Transaction[] = [];
  selectedFilter: 'ALL' | 'ARS' | 'USD' = 'ALL';
  selectedTransaction: Transaction | null = null;
  showTransactionDetail = false;
  receiptBusy = false;

  private subscriptions: Subscription[] = [];

  formatMoney = formatMoney;

  constructor(
    private transactionService: TransactionService,
    private receiptPdf: ReceiptPdfService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.subscriptions.push(
      this.transactionService.allTransactions$.subscribe((transactions) => {
        this.usdTransactions = transactions.filter(
          (t) =>
            t.kind === 'buy_usd' ||
            t.kind === 'sell_usd' ||
            t.currency === 'USD' ||
            t.originalCurrency === 'USD' ||
            t.description.toLowerCase().includes('dólar') ||
            t.description.toLowerCase().includes('dolar')
        );
        this.applyFilter();
      })
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['enabled'] && this.enabled) {
      this.reload();
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((s) => s.unsubscribe());
  }

  reload(): void {
    if (this.enabled) {
      void this.transactionService.loadAllTransactions(true);
    }
  }

  setFilter(filter: 'ALL' | 'ARS' | 'USD'): void {
    this.selectedFilter = filter;
    this.applyFilter();
  }

  openTransactionDetail(transaction: Transaction): void {
    this.selectedTransaction = transaction;
    this.showTransactionDetail = true;
  }

  closeTransactionDetail(): void {
    this.showTransactionDetail = false;
    this.selectedTransaction = null;
  }

  formatDate(date: Date): string {
    return formatDateTime(date);
  }

  getDisplayAmount(transaction: Transaction): number {
    if (transaction.currency === 'USD' && transaction.originalCurrency === 'USD') {
      return transaction.amount;
    }
    return transaction.amountInArs || transaction.amount;
  }

  getDisplayCurrency(transaction: Transaction): string {
    if (transaction.currency === 'USD' && transaction.originalCurrency === 'USD') {
      return 'USD';
    }
    return 'ARS';
  }

  getTransferType(transaction: Transaction): string {
    if (transaction.currency === 'USD' && transaction.originalCurrency === 'USD') {
      return 'Transferencia directa USD → USD';
    }
    if (transaction.currency === 'USD' && transaction.originalCurrency !== 'USD') {
      return 'Transferencia desde cuenta USD usando ARS';
    }
    if (transaction.originalCurrency === 'USD' && transaction.currency !== 'USD') {
      return 'Transferencia a cuenta USD usando ARS';
    }
    return 'Transferencia en pesos';
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
      this.toast.show(
        result === 'shared' ? 'Comprobante listo para compartir' : 'Comprobante descargado para que lo compartas',
        'success'
      );
    } catch (error: unknown) {
      if (error instanceof DOMException && error.name === 'AbortError') return;
      logger.error('Error compartiendo PDF', error);
      this.toast.show('No se pudo compartir el comprobante', 'error');
    } finally {
      this.receiptBusy = false;
    }
  }

  private receiptLabels(tx: Transaction) {
    const sign = tx.type === 'income' ? '+' : '-';
    const code = this.getDisplayCurrency(tx);
    const amount = this.getDisplayAmount(tx);
    return {
      title: 'Detalle de la operación',
      description: tx.description,
      amountLabel: `${sign}$${formatMoney(amount)} ${code}`,
      origin: tx.from || 'Tu cuenta',
      destination: tx.to || 'Cuenta destino',
    };
  }

  private applyFilter(): void {
    if (this.selectedFilter === 'ALL') {
      this.filteredTransactions = this.usdTransactions;
    } else if (this.selectedFilter === 'USD') {
      this.filteredTransactions = this.usdTransactions.filter(
        (t) => t.currency === 'USD' || t.originalCurrency === 'USD'
      );
    } else {
      this.filteredTransactions = this.usdTransactions.filter(
        (t) => !t.currency || t.currency === 'ARS'
      );
    }
  }
}
