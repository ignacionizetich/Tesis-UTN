import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription, of, from } from 'rxjs';
import { tap } from 'rxjs/operators';

// Services
import { AccountService } from '../../services/account-service/account.service';
import { UserDataStore } from '../../services/user-data-store/user-data.store';
import { ToastService } from '../../services/toast-service/toast.service';
import { TransactionService } from '../../services/transaction-service/transaction-service';
import { AccountPollingCoordinator } from '../../services/account-polling/account-polling.coordinator';

// Feature components
import { BuyUsdPanelComponent } from './components/buy-usd-panel/buy-usd-panel';
import { SellUsdPanelComponent } from './components/sell-usd-panel/sell-usd-panel';
import { TransferWizardComponent } from '../dashboard/components/transfer-wizard/transfer-wizard';

// Models / utils
import Transaction from '../../models/transaction';
import UserData from '../../models/user-data';
import { formatMoney as formatMoneyShared } from '../../shared/utils/money-format';
import { formatDateTime } from '../../shared/utils/date-format';

@Component({
  selector: 'app-usd-account',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    BuyUsdPanelComponent,
    SellUsdPanelComponent,
    TransferWizardComponent,
  ],
  templateUrl: './usd-account.html',
  styleUrls: ['./usd-account.css']
})
export class UsdAccountComponent implements OnInit, OnDestroy {

  private subscriptions: Subscription[] = [];
  private dataPollingSubscription: Subscription | null = null;

  isLoading = true;
  isCreatingUsdAccount = false;
  isBuyingUsd = false;
  isSellingUsd = false;

  hasUsdAccount = false;
  usdAccountId = '';
  usdBalance = 0;
  usdAlias = '';
  usdCvu = '';
  arsAccountId = '';
  arsBalance = 0;

  userData: UserData = {
    name: 'Cargando...', lastName: '', dni: '', email: '', alias: '',
    cvu: '', username: '', balance: 0, idAccount: ''
  };

  usdTransactions: Transaction[] = [];
  filteredTransactions: Transaction[] = [];
  selectedFilter: 'ALL' | 'ARS' | 'USD' = 'ALL';
  selectedTransaction: Transaction | null = null;
  showTransactionDetail = false;

  showBuyUsdSection = false;
  showSellUsdSection = false;
  showTransferSection = false;
  showReceiveSection = false;

  amountToBuyUsd: number | null = null;
  amountToSellUsd: number | null = null;

  readonly taxRate = 0.03;
  readonly taxPercentage = 3;
  estimatedUsdAmount = 0;
  estimatedArsAmount = 0;
  estimatedTaxAmount = 0;
  estimatedTotalDebitado = 0;
  currentExchangeRate = 0;

  exchangeRate = 0;

  get arsAccountView(): { balance: number } | null {
    return this.arsAccountId ? { balance: this.arsBalance } : null;
  }

  get usdAccountView(): { balance: number } | null {
    return this.hasUsdAccount ? { balance: this.usdBalance } : null;
  }

  constructor(
    private router: Router,
    private accountService: AccountService,
    private userDataStore: UserDataStore,
    private toast: ToastService,
    private transactionService: TransactionService,
    private accountPolling: AccountPollingCoordinator
  ) {}

  ngOnInit(): void {
    this.loadUserData();
    this.checkUsdAccount();
    this.loadExchangeRate();
    this.startDataPolling(10000);
  }

  ngOnDestroy(): void {
    this.stopDataPolling();
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private loadUserData(): void {
    const userDataSub = this.userDataStore.userData$.subscribe(userData => {
      if (userData) {
        this.userData = userData;
        this.arsBalance = userData.balance;
        this.arsAccountId = userData.idAccount;
      }
      this.isLoading = false;
    });
    this.subscriptions.push(userDataSub);

    this.userDataStore.load(true).subscribe();
  }

  private checkUsdAccount(): void {
    this.accountService.getUserAccounts().subscribe({
      next: (accounts) => {
        const usdAccount = accounts.find(acc => acc.currency === 'USD');
        if (usdAccount) {
          this.hasUsdAccount = true;
          this.usdAccountId = usdAccount.id;
          this.usdBalance = usdAccount.balance;
          this.usdAlias = usdAccount.alias;
          this.usdCvu = usdAccount.cvu;
          this.loadTransactions();
        } else {
          this.hasUsdAccount = false;
        }
      },
      error: (error) => {
        console.error('Error verificando cuenta USD:', error);
        this.hasUsdAccount = false;
      }
    });
  }

  async createUsdAccount(): Promise<void> {
    this.isCreatingUsdAccount = true;

    try {
      const response = await this.accountService.openUsdAccount().toPromise();

      if (response.success) {
        this.toast.show('Cuenta en dólares creada exitosamente', 'success');
        this.usdAccountId = response.accountId;
        this.hasUsdAccount = true;
        this.usdBalance = 0;
        this.checkUsdAccount();
      } else {
        this.toast.show(response.message || 'Error al crear cuenta en dólares', 'error');
      }
    } catch (error: any) {
      console.error('Error creando cuenta USD:', error);
      this.toast.show(error.error?.message || 'Error al crear cuenta en dólares', 'error');
    } finally {
      this.isCreatingUsdAccount = false;
    }
  }

  openBuyUsdSection(): void {
    this.closeSections();
    this.showBuyUsdSection = true;
  }

  closeBuyUsdSection(): void {
    this.showBuyUsdSection = false;
    this.amountToBuyUsd = null;
    this.resetConversionPreview();
  }

  async buyUsd(): Promise<void> {
    if (!this.amountToBuyUsd || this.amountToBuyUsd <= 0) {
      this.toast.show('Por favor ingrese un monto válido', 'error');
      return;
    }

    const totalDebitado = this.amountToBuyUsd * (1 + this.taxRate);
    if (totalDebitado > this.arsBalance) {
      this.toast.show(
        `Saldo insuficiente. Necesitás $${totalDebitado.toFixed(2)} ARS (incluye comisión del ${this.taxPercentage}%)`,
        'error'
      );
      return;
    }

    this.isBuyingUsd = true;

    try {
      const response = await this.accountService.buyUsd(
        this.arsAccountId,
        this.usdAccountId,
        this.amountToBuyUsd
      ).toPromise();

      if (response && response.success) {
        if (response.exchangeRate) {
          this.currentExchangeRate = response.exchangeRate;
        }

        this.toast.show(
          `Compra exitosa: $${response.amountUsd.toFixed(2)} USD`,
          'success'
        );

        this.arsBalance = response.newBalanceArs;
        this.usdBalance = response.newBalanceUsd;
        this.userData.balance = response.newBalanceArs;

        this.closeBuyUsdSection();
        this.loadTransactions();
        this.userDataStore.load(true).subscribe();
      } else {
        this.toast.show(response?.message || 'Error en la compra', 'error');
      }
    } catch (error: any) {
      console.error('Error comprando USD:', error);
      this.toast.show(
        error.error?.message || 'Error al comprar dólares',
        'error'
      );
    } finally {
      this.isBuyingUsd = false;
    }
  }

  openSellUsdSection(): void {
    this.closeSections();
    this.showSellUsdSection = true;
  }

  closeSellUsdSection(): void {
    this.showSellUsdSection = false;
    this.amountToSellUsd = null;
    this.resetConversionPreview();
  }

  async sellUsd(): Promise<void> {
    if (!this.amountToSellUsd || this.amountToSellUsd <= 0) {
      this.toast.show('Por favor ingrese un monto válido', 'error');
      return;
    }

    const totalDebitado = this.amountToSellUsd * (1 + this.taxRate);
    if (totalDebitado > this.usdBalance) {
      this.toast.show(
        `Saldo insuficiente. Necesitás $${totalDebitado.toFixed(2)} USD (incluye comisión del ${this.taxPercentage}%)`,
        'error'
      );
      return;
    }

    this.isSellingUsd = true;

    try {
      const response = await this.accountService.sellUsd(
        this.usdAccountId,
        this.arsAccountId,
        this.amountToSellUsd
      ).toPromise();

      if (response && response.success) {
        if (response.exchangeRate) {
          this.currentExchangeRate = response.exchangeRate;
        }

        this.toast.show(
          `Venta exitosa: $${response.amountArs.toFixed(2)} ARS`,
          'success'
        );

        this.usdBalance = response.newBalanceUsd;
        this.arsBalance = response.newBalanceArs;
        this.userData.balance = response.newBalanceArs;

        this.closeSellUsdSection();
        this.loadTransactions();
        this.userDataStore.load(true).subscribe();
      } else {
        this.toast.show(response?.message || 'Error en la venta', 'error');
      }
    } catch (error: any) {
      console.error('Error vendiendo USD:', error);
      this.toast.show(
        error.error?.message || 'Error al vender dólares',
        'error'
      );
    } finally {
      this.isSellingUsd = false;
    }
  }

  openTransferSection(): void {
    this.closeSections();
    this.showTransferSection = true;
  }

  closeTransferSection(): void {
    this.showTransferSection = false;
  }

  onTransferAccountsReload(): void {
    this.checkUsdAccount();
    this.loadTransactions();
  }

  openReceiveSection(): void {
    this.closeSections();
    this.showReceiveSection = true;
  }

  closeReceiveSection(): void {
    this.showReceiveSection = false;
  }

  copyToClipboard(text: string, label: string): void {
    navigator.clipboard.writeText(text).then(() => {
      this.toast.show(`${label} copiado al portapapeles`, 'success');
    }).catch(() => {
      this.toast.show('Error al copiar', 'error');
    });
  }

  private closeSections(): void {
    this.showBuyUsdSection = false;
    this.showSellUsdSection = false;
    this.showTransferSection = false;
    this.showReceiveSection = false;
  }

  private loadTransactions(): void {
    if (this.usdAccountId) {
      this.transactionService.loadAllTransactions(true);
      const transSub = this.transactionService.allTransactions$.subscribe(transactions => {
        this.usdTransactions = transactions.filter(t =>
          t.description.includes('USD') ||
          t.description.includes('dólar') ||
          t.currency === 'USD'
        );
        this.applyFilter();
      });
      this.subscriptions.push(transSub);
    }
  }

  private startDataPolling(intervalMs: number = 10000): void {
    this.stopDataPolling();

    this.dataPollingSubscription = this.accountPolling.start(
      intervalMs,
      [
        () => this.userDataStore.load(true),
        () => {
          if (!this.hasUsdAccount) {
            return of(null);
          }
          return this.accountService.getUserAccounts().pipe(
            tap((accounts) => {
              const usdAccount = accounts.find((acc) => acc.currency === 'USD');
              if (usdAccount) {
                this.usdBalance = usdAccount.balance;
                this.usdAccountId = usdAccount.id;
                this.usdAlias = usdAccount.alias;
                this.usdCvu = usdAccount.cvu;
              }
            })
          );
        },
        () =>
          this.hasUsdAccount
            ? from(this.transactionService.loadAllTransactions(true))
            : of(null),
      ],
      {
        onError: (err) => {
          console.error('>>> Polling: Error durante la actualización de datos USD:', err);
        },
      }
    );

    this.subscriptions.push(this.dataPollingSubscription);
  }

  private stopDataPolling(): void {
    if (this.dataPollingSubscription) {
      this.dataPollingSubscription.unsubscribe();
      this.dataPollingSubscription = null;
    }
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  formatMoney(amount: number): string {
    return formatMoneyShared(amount);
  }

  formatDate(date: Date): string {
    return formatDateTime(date);
  }

  getCurrencyLabel(currency: string): string {
    return currency === 'ARS' ? 'Pesos' : 'Dólares';
  }

  private loadExchangeRate(): void {
    this.accountService.getUserAccounts().subscribe({
      next: () => {
        this.currentExchangeRate = 1100;
      },
      error: (error) => {
        console.error('Error cargando tipo de cambio:', error);
        this.currentExchangeRate = 1100;
      }
    });
  }

  private resetConversionPreview(): void {
    this.estimatedUsdAmount = 0;
    this.estimatedArsAmount = 0;
    this.estimatedTaxAmount = 0;
    this.estimatedTotalDebitado = 0;
  }

  onAmountToBuyChange(): void {
    if (this.amountToBuyUsd && this.amountToBuyUsd > 0) {
      const amountArs = this.amountToBuyUsd;
      this.estimatedTaxAmount = amountArs * this.taxRate;
      this.estimatedTotalDebitado = amountArs + this.estimatedTaxAmount;
      this.estimatedUsdAmount = this.currentExchangeRate > 0
        ? amountArs / this.currentExchangeRate
        : 0;
    } else {
      this.resetConversionPreview();
    }
  }

  onAmountToSellChange(): void {
    if (this.amountToSellUsd && this.amountToSellUsd > 0) {
      const amountUsd = this.amountToSellUsd;
      this.estimatedTaxAmount = amountUsd * this.taxRate;
      this.estimatedTotalDebitado = amountUsd + this.estimatedTaxAmount;
      this.estimatedArsAmount = amountUsd * this.currentExchangeRate;
    } else {
      this.resetConversionPreview();
    }
  }

  applyFilter(): void {
    if (this.selectedFilter === 'ALL') {
      this.filteredTransactions = this.usdTransactions;
    } else if (this.selectedFilter === 'USD') {
      this.filteredTransactions = this.usdTransactions.filter(t =>
        t.currency === 'USD' || t.originalCurrency === 'USD'
      );
    } else if (this.selectedFilter === 'ARS') {
      this.filteredTransactions = this.usdTransactions.filter(t =>
        !t.currency || t.currency === 'ARS'
      );
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

  getDisplayAmount(transaction: Transaction): number {
    if (transaction.currency === 'USD' && transaction.originalCurrency === 'USD') {
      return transaction.amount;
    }

    if (transaction.amountInArs) {
      return transaction.amountInArs;
    }

    return transaction.amount;
  }

  getDisplayCurrency(transaction: Transaction): string {
    if (transaction.currency === 'USD' && transaction.originalCurrency === 'USD') {
      return 'USD';
    }

    return 'ARS';
  }

  shouldShowSecondaryAmount(transaction: Transaction): boolean {
    return !!(
      transaction.currency === 'USD' &&
      transaction.originalCurrency === 'USD' &&
      transaction.amountInArs
    );
  }

  getTransferType(transaction: Transaction): string {
    if (transaction.currency === 'USD' && transaction.originalCurrency === 'USD') {
      return 'Transferencia directa USD → USD';
    } else if (transaction.currency === 'USD' && transaction.originalCurrency !== 'USD') {
      return 'Transferencia desde cuenta USD usando ARS';
    } else if (transaction.originalCurrency === 'USD' && transaction.currency !== 'USD') {
      return 'Transferencia a cuenta USD usando ARS';
    }
    return 'Transferencia en pesos';
  }
}
