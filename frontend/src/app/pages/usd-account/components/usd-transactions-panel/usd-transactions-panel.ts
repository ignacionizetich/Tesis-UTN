import { Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import Transaction from '../../../../models/transaction';
import { TransactionService } from '../../../../services/transaction/transaction.service';
import { formatMoney } from '../../../../shared/utils/money-format';
import { formatDateTime } from '../../../../shared/utils/date-format';

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

  private subscriptions: Subscription[] = [];

  formatMoney = formatMoney;

  constructor(private transactionService: TransactionService) {}

  ngOnInit(): void {
    this.subscriptions.push(
      this.transactionService.allTransactions$.subscribe((transactions) => {
        this.usdTransactions = transactions.filter(
          (t) =>
            t.description.includes('USD') ||
            t.description.includes('dólar') ||
            t.currency === 'USD'
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
