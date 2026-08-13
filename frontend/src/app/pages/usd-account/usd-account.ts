import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription, of, from } from 'rxjs';
import { tap } from 'rxjs/operators';

import { AccountService } from '../../services/account/account.service';
import { UserDataStore } from '../../services/user-data-store/user-data.store';
import { ToastService } from '../../services/toast/toast.service';
import { TransactionService } from '../../services/transaction/transaction.service';
import { AccountPollingCoordinator } from '../../services/account-polling/account-polling.coordinator';

import {
  BuyUsdPanelComponent,
  UsdTradeSuccess,
} from './components/buy-usd-panel/buy-usd-panel';
import { SellUsdPanelComponent } from './components/sell-usd-panel/sell-usd-panel';
import { ReceiveUsdModalComponent } from './components/receive-usd-modal/receive-usd-modal';
import { UsdTransactionsPanelComponent } from './components/usd-transactions-panel/usd-transactions-panel';
import { TransferWizardComponent } from '../dashboard/components/transfer-wizard/transfer-wizard';

import UserData from '../../models/user-data';
import { formatMoney as formatMoneyShared } from '../../shared/utils/money-format';
import { errorMessage } from '../../shared/utils/error-message';

@Component({
  selector: 'app-usd-account',
  standalone: true,
  imports: [
    CommonModule,
    BuyUsdPanelComponent,
    SellUsdPanelComponent,
    ReceiveUsdModalComponent,
    UsdTransactionsPanelComponent,
    TransferWizardComponent,
  ],
  templateUrl: './usd-account.html',
  styleUrls: ['./usd-account.css'],
})
export class UsdAccountComponent implements OnInit, OnDestroy {
  @ViewChild(UsdTransactionsPanelComponent)
  private transactionsPanel?: UsdTransactionsPanelComponent;

  private subscriptions: Subscription[] = [];
  private dataPollingSubscription: Subscription | null = null;

  isLoading = true;
  isCreatingUsdAccount = false;

  hasUsdAccount = false;
  usdAccountId = '';
  usdBalance = 0;
  usdAlias = '';
  usdCvu = '';
  arsAccountId = '';
  arsBalance = 0;

  userData: UserData = {
    name: 'Cargando...',
    lastName: '',
    dni: '',
    email: '',
    alias: '',
    cvu: '',
    username: '',
    balance: 0,
    idAccount: '',
  };

  showBuyUsdSection = false;
  showSellUsdSection = false;
  showTransferSection = false;
  showReceiveSection = false;

  readonly taxRate = 0.03;
  readonly taxPercentage = 3;
  /** Estimación local hasta que el backend exponga cotización. */
  currentExchangeRate = 1100;

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
    this.startDataPolling(10000);
  }

  ngOnDestroy(): void {
    this.stopDataPolling();
    this.subscriptions.forEach((sub) => sub.unsubscribe());
  }

  formatMoney(amount: number): string {
    return formatMoneyShared(amount);
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  async createUsdAccount(): Promise<void> {
    this.isCreatingUsdAccount = true;
    try {
      const response = await this.accountService.openUsdAccount().toPromise();
      if (response?.success) {
        this.toast.show('Cuenta en dólares creada exitosamente', 'success');
        this.usdAccountId = String(response.accountId ?? '');
        this.hasUsdAccount = true;
        this.usdBalance = 0;
        this.checkUsdAccount();
      } else {
        this.toast.show(response?.message || 'Error al crear cuenta en dólares', 'error');
      }
    } catch (error: unknown) {
      console.error('Error creando cuenta USD:', error);
      this.toast.show(errorMessage(error, 'Error al crear cuenta en dólares'), 'error');
    } finally {
      this.isCreatingUsdAccount = false;
    }
  }

  openBuyUsdSection(): void {
    this.closeSections();
    this.showBuyUsdSection = true;
  }

  openSellUsdSection(): void {
    this.closeSections();
    this.showSellUsdSection = true;
  }

  openTransferSection(): void {
    this.closeSections();
    this.showTransferSection = true;
  }

  openReceiveSection(): void {
    this.closeSections();
    this.showReceiveSection = true;
  }

  closeBuyUsdSection(): void {
    this.showBuyUsdSection = false;
  }

  closeSellUsdSection(): void {
    this.showSellUsdSection = false;
  }

  closeTransferSection(): void {
    this.showTransferSection = false;
  }

  closeReceiveSection(): void {
    this.showReceiveSection = false;
  }

  onTradeSuccess(result: UsdTradeSuccess): void {
    this.arsBalance = result.newBalanceArs;
    this.usdBalance = result.newBalanceUsd;
    this.userData.balance = result.newBalanceArs;
    if (result.exchangeRate) {
      this.currentExchangeRate = result.exchangeRate;
    }
    this.transactionsPanel?.reload();
    this.userDataStore.load(true).subscribe();
  }

  onTransferAccountsReload(): void {
    this.checkUsdAccount();
    this.transactionsPanel?.reload();
  }

  private loadUserData(): void {
    this.subscriptions.push(
      this.userDataStore.userData$.subscribe((userData) => {
        if (userData) {
          this.userData = userData;
          this.arsBalance = userData.balance;
          this.arsAccountId = userData.idAccount;
        }
        this.isLoading = false;
      })
    );
    this.userDataStore.load(true).subscribe();
  }

  private checkUsdAccount(): void {
    this.accountService.getUserAccounts().subscribe({
      next: (accounts) => {
        const usdAccount = accounts.find((acc) => acc.currency === 'USD');
        if (usdAccount) {
          this.hasUsdAccount = true;
          this.usdAccountId = usdAccount.id;
          this.usdBalance = usdAccount.balance;
          this.usdAlias = usdAccount.alias;
          this.usdCvu = usdAccount.cvu;
          this.transactionsPanel?.reload();
        } else {
          this.hasUsdAccount = false;
        }
      },
      error: (error) => {
        console.error('Error verificando cuenta USD:', error);
        this.hasUsdAccount = false;
      },
    });
  }

  private closeSections(): void {
    this.showBuyUsdSection = false;
    this.showSellUsdSection = false;
    this.showTransferSection = false;
    this.showReceiveSection = false;
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
}
