import { Component, OnInit, OnDestroy, ChangeDetectorRef, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser, CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription, of, from, firstValueFrom } from 'rxjs';

import { ThemeService } from '../../services/theme/theme.service';
import { ToastService } from '../../services/toast/toast.service';
import { AuthService } from '../../services/auth/auth.service';
import { UserDataStore } from '../../services/user-data-store/user-data.store';
import { TransactionHistoryStore } from '../../services/transaction-history-store/transaction-history.store';
import { ModalService } from '../../services/modal/modal.service';
import { TransactionService } from '../../services/transaction/transaction.service';
import { FavoriteService } from '../../services/favorite/favorite.service';
import { AdminService } from '../../services/admin/admin.service';
import { AccountService, UserAccount } from '../../services/account/account.service';
import { SessionStore } from '../../core/session/session.store';
import { AccountPollingCoordinator } from '../../services/account-polling/account-polling.coordinator';
import { formatMoney as formatMoneyShared } from '../../shared/utils/money-format';
import { errorMessage } from '../../shared/utils/error-message';
import { FavoriteContact } from '../../models/favorite-contact';

import { DepositModalComponent } from './components/deposit-modal/deposit-modal';
import {
  TransferWizardComponent,
  TransferWizardSeed,
} from './components/transfer-wizard/transfer-wizard';
import { AliasModalComponent } from './components/alias-modal/alias-modal';
import { TaxModalComponent } from './components/tax-modal/tax-modal';
import { ProfileModalComponent } from './components/profile-modal/profile-modal';
import { ReceiveQrModalComponent } from './components/receive-qr-modal/receive-qr-modal';
import {
  FavoritesModalsComponent,
  TransferCompletedData,
} from './components/favorites-modals/favorites-modals';
import { CardsModalsComponent } from './components/cards-modals/cards-modals';
import { LoansModalsComponent } from './components/loans-modals/loans-modals';
import { TransactionsPanelComponent } from './components/transactions-panel/transactions-panel';
import {
  BuyUsdPanelComponent,
  UsdTradeSuccess,
} from '../usd-account/components/buy-usd-panel/buy-usd-panel';
import { SellUsdPanelComponent } from '../usd-account/components/sell-usd-panel/sell-usd-panel';
import { ReceiveUsdModalComponent } from '../usd-account/components/receive-usd-modal/receive-usd-modal';

import UserData from '../../models/user-data';
import { logger } from '../../shared/utils/logger';

type WalletCurrency = 'ARS' | 'USD';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DepositModalComponent,
    TransferWizardComponent,
    AliasModalComponent,
    TaxModalComponent,
    ProfileModalComponent,
    ReceiveQrModalComponent,
    FavoritesModalsComponent,
    CardsModalsComponent,
    LoansModalsComponent,
    TransactionsPanelComponent,
    BuyUsdPanelComponent,
    SellUsdPanelComponent,
    ReceiveUsdModalComponent,
  ],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css'],
})
export class DashboardComponent implements OnInit, OnDestroy {
  private subscriptions: Subscription[] = [];
  private dataPollingSubscription: Subscription | null = null;

  isLoading = true;
  balanceVisible = true;
  isAdmin = false;

  activeCurrency: WalletCurrency = 'ARS';
  moreMenuOpen = false;
  showBuyUsd = false;
  showSellUsd = false;
  showReceiveUsd = false;
  isCreatingUsdAccount = false;

  readonly taxRate = 0.03;
  readonly taxPercentage = 3;
  exchangeRate = 1100;

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

  currentModal: string | null = null;

  transferCompletedData: TransferCompletedData | null = null;
  transferSeed: TransferWizardSeed | null = null;

  userAccounts: UserAccount[] = [];
  arsAccount: UserAccount | null = null;
  usdAccount: UserAccount | null = null;

  isBalanceUpdating = false;
  isBalanceDecreasing = false;

  constructor(
    private router: Router,
    private themeService: ThemeService,
    private toast: ToastService,
    private authService: AuthService,
    private userDataStore: UserDataStore,
    private transactionHistoryStore: TransactionHistoryStore,
    private modalService: ModalService,
    private transactionService: TransactionService,
    private favoriteService: FavoriteService,
    private adminService: AdminService,
    private accountService: AccountService,
    private sessionStore: SessionStore,
    private accountPolling: AccountPollingCoordinator,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  get otherCurrency(): WalletCurrency {
    return this.activeCurrency === 'ARS' ? 'USD' : 'ARS';
  }

  get displayBalance(): number {
    if (this.activeCurrency === 'USD') {
      return this.usdAccount?.balance ?? 0;
    }
    return this.arsAccount?.balance ?? this.userData.balance ?? 0;
  }

  get currencySymbol(): string {
    return this.activeCurrency === 'USD' ? 'U$S' : '$';
  }

  get displayAlias(): string {
    if (this.activeCurrency === 'USD' && this.usdAccount) {
      return this.usdAccount.alias || this.userData.alias;
    }
    return this.arsAccount?.alias || this.userData.alias;
  }

  get displayCvu(): string {
    if (this.activeCurrency === 'USD' && this.usdAccount) {
      return this.usdAccount.cvu || this.userData.cvu;
    }
    return this.arsAccount?.cvu || this.userData.cvu;
  }

  get arsAccountId(): string {
    return this.arsAccount?.id ? String(this.arsAccount.id) : String(this.userData.idAccount || '');
  }

  get usdAccountId(): string {
    return this.usdAccount?.id ? String(this.usdAccount.id) : '';
  }

  /** Cuenta cuyo extracto muestra el panel de movimientos (según toggle). */
  get activeAccountId(): string {
    if (this.activeCurrency === 'USD' && this.usdAccountId) {
      return this.usdAccountId;
    }
    return this.arsAccountId;
  }

  get arsBalance(): number {
    return this.arsAccount?.balance ?? this.userData.balance ?? 0;
  }

  get usdBalance(): number {
    return this.usdAccount?.balance ?? 0;
  }

  ngOnInit(): void {
    this.checkAuthentication();
    this.checkAdminRole();
    this.setupSubscriptions();

    this.userDataStore.load(true).subscribe({
      next: (data) => {
        if (!data) {
          logger.error('>>> Dashboard ngOnInit: loadUserData inicial devolvió null.');
        }
      },
      error: (err) =>
        logger.error('>>> Dashboard ngOnInit: ERROR crítico en loadUserData inicial:', err),
    });

    this.initializeServices();
    this.startSimpleLoading();

    if (isPlatformBrowser(this.platformId)) {
      this.startDataPolling(10000);
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((sub) => sub.unsubscribe());
    this.stopDataPolling();
  }

  private startDataPolling(intervalMs: number = 10000): void {
    this.stopDataPolling();

    this.dataPollingSubscription = this.accountPolling.start(
      intervalMs,
      [
        () => this.userDataStore.load(true),
        () => from(this.loadUserAccounts()),
        () => {
          if (this.modalService.getCurrentModal() === 'allTransactions') {
            return of(null);
          }
          return from(
            this.transactionService.loadAllTransactions(true, this.activeAccountId)
          );
        },
      ],
      {
        onError: (err) => {
          logger.error('>>> Polling: Error durante la actualización de datos:', err);
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

  private async initializeServices(): Promise<void> {
    try {
      await Promise.all([
        this.loadUserAccounts(),
        this.favoriteService.loadFavoriteContacts(),
      ]);
      await this.transactionService.loadAllTransactions(true, this.activeAccountId);
    } catch (error) {
      logger.error('Error inicializando services:', error);
    }
  }

  private startSimpleLoading(): void {
    setTimeout(() => {
      this.isLoading = false;
    }, 1500);
    this.loadDataInBackground();
  }

  private setupSubscriptions(): void {
    const userDataSub = this.userDataStore.userData$.subscribe((userDataFromService) => {
      if (userDataFromService) {
        this.userData = userDataFromService;
      } else {
        this.userData = {
          name: '',
          lastName: '',
          dni: '',
          email: '',
          alias: '',
          cvu: '',
          username: '',
          balance: 0,
          idAccount: '',
        };
      }
      this.cdr.detectChanges();
    });
    this.subscriptions.push(userDataSub);

    const modalSub = this.modalService.modalState$.subscribe((state) => {
      this.currentModal = state.currentModal;
      this.cdr.detectChanges();
    });
    this.subscriptions.push(modalSub);
  }

  async loadUserAccounts(): Promise<void> {
    try {
      this.userAccounts = (await this.accountService.getUserAccounts().toPromise()) || [];
      this.arsAccount = this.userAccounts.find((acc) => acc.currency === 'ARS') || null;
      this.usdAccount = this.userAccounts.find((acc) => acc.currency === 'USD') || null;
      this.cdr.detectChanges();
    } catch (error) {
      logger.error('Error cargando cuentas del usuario:', error);
    }
  }

  private async loadDataInBackground(): Promise<void> {
    try {
      await Promise.allSettled([
        this.userDataStore.load(true),
        this.transactionHistoryStore.load(this.activeAccountId),
      ]);
    } catch (error) {
      logger.error('Error cargando datos:', error);
    }
  }

  checkAuthentication(): void {
    if (!this.sessionStore.hasAccessToken()) {
      this.router.navigate(['/login']);
    }
  }

  checkAdminRole(): void {
    this.isAdmin = this.adminService.isAdmin();
  }

  async goToAdminPanel(): Promise<void> {
    if (!this.sessionStore.isAdmin()) {
      this.toast.show(
        'No tienes permisos para acceder al panel de administración',
        'error'
      );
      return;
    }

    this.isLoading = true;

    try {
      await this.adminService.checkAccess().toPromise();
      this.router.navigate(['/admin']);
    } catch (error: unknown) {
      logger.error('Error al verificar acceso de admin:', error);
      const status = error && typeof error === 'object' && 'status' in error
        ? (error as { status?: number }).status
        : undefined;
      if (status === 403 || status === 401) {
        this.toast.show(
          'No tienes permisos para acceder al panel de administración',
          'error'
        );
      } else if (status === 0) {
        this.toast.show(
          'No se puede conectar con el servidor. Verifica que el backend esté ejecutándose.',
          'error'
        );
      } else {
        this.toast.show(errorMessage(error, 'Error del servidor. Intenta más tarde.'), 'error');
      }
    } finally {
      this.isLoading = false;
    }
  }

  toggleCurrency(): void {
    this.activeCurrency = this.otherCurrency;
    if (this.activeAccountId) {
      void this.transactionService.loadAllTransactions(true, this.activeAccountId);
    }
  }

  async createUsdAccount(): Promise<void> {
    this.isCreatingUsdAccount = true;
    try {
      const response = await this.accountService.openUsdAccount().toPromise();
      if (response?.success) {
        this.toast.show('Cuenta en dólares creada exitosamente', 'success');
        await this.loadUserAccounts();
      } else {
        this.toast.show(response?.message || 'Error al crear cuenta en dólares', 'error');
      }
    } catch (error: unknown) {
      logger.error('Error creando cuenta USD:', error);
      this.toast.show(errorMessage(error, 'Error al crear cuenta en dólares'), 'error');
    } finally {
      this.isCreatingUsdAccount = false;
      this.cdr.detectChanges();
    }
  }

  openMoreMenu(): void {
    this.moreMenuOpen = true;
  }

  closeMoreMenu(): void {
    this.moreMenuOpen = false;
  }

  onMoreAction(action: 'alias' | 'favorites' | 'cards' | 'loans' | 'tax' | 'buy' | 'sell'): void {
    this.closeMoreMenu();
    switch (action) {
      case 'alias':
        this.openAliasModal();
        break;
      case 'favorites':
        void this.openFavoritesModal();
        break;
      case 'cards':
        this.openModal('cards');
        break;
      case 'loans':
        this.openModal('loans');
        break;
      case 'tax':
        this.openTaxModal();
        break;
      case 'buy':
        this.openBuyUsd();
        break;
      case 'sell':
        this.openSellUsd();
        break;
    }
  }

  openBuyUsd(): void {
    if (!this.usdAccount) {
      this.activeCurrency = 'USD';
      this.toast.show('Primero abrí tu cuenta en dólares', 'info');
      return;
    }
    if (!this.arsAccountId) {
      this.toast.show('No se encontró la cuenta en pesos', 'error');
      return;
    }
    this.showSellUsd = false;
    this.showBuyUsd = true;
  }

  closeBuyUsd(): void {
    this.showBuyUsd = false;
  }

  openSellUsd(): void {
    if (!this.usdAccount) {
      this.toast.show('No tenés cuenta en dólares', 'error');
      return;
    }
    this.showBuyUsd = false;
    this.showSellUsd = true;
  }

  closeSellUsd(): void {
    this.showSellUsd = false;
  }

  openReceiveAction(): void {
    if (this.activeCurrency === 'USD' && !this.usdAccount) {
      this.toast.show('Primero abrí tu cuenta en dólares', 'info');
      return;
    }
    this.openMyQrModal();
  }

  closeReceiveUsd(): void {
    this.showReceiveUsd = false;
  }

  async onTradeSuccess(result: UsdTradeSuccess): Promise<void> {
    if (this.arsAccount) {
      this.arsAccount = { ...this.arsAccount, balance: result.newBalanceArs };
    }
    if (this.usdAccount) {
      this.usdAccount = { ...this.usdAccount, balance: result.newBalanceUsd };
    }
    if (result.exchangeRate) {
      this.exchangeRate = result.exchangeRate;
    }
    this.userData = { ...this.userData, balance: result.newBalanceArs };
    this.isBalanceUpdating = true;
    setTimeout(() => {
      this.isBalanceUpdating = false;
      this.cdr.detectChanges();
    }, 1200);
    await this.loadUserAccounts();
    void this.userDataStore.load(true).toPromise();
    void this.transactionService.loadAllTransactions(true, this.activeAccountId);
    this.cdr.detectChanges();
  }

  onDepositSuccess(): void {
    this.isBalanceUpdating = true;
    void this.loadUserAccounts().finally(() => {
      setTimeout(() => {
        this.isBalanceUpdating = false;
        this.cdr.detectChanges();
      }, 1400);
    });
    void firstValueFrom(this.userDataStore.load(true)).catch(() => undefined);
  }

  private closeAllModals(): void {
    this.modalService.closeModal();
  }

  private openModal(modalType: string): void {
    this.modalService.openModal(modalType);
  }

  openIngresarModal(): void {
    this.openModal('ingresar');
  }

  closeIngresarModal(): void {
    this.closeAllModals();
    this.isBalanceUpdating = false;
    this.isBalanceDecreasing = false;
  }

  closeMyQrModal(): void {
    this.currentModal = null;
  }

  openTransferModal(): void {
    if (this.activeCurrency === 'USD' && !this.usdAccount) {
      this.toast.show('Primero abrí tu cuenta en dólares', 'info');
      return;
    }
    this.transferSeed = { preferredCurrency: this.activeCurrency };
    this.isBalanceDecreasing = false;
    this.openModal('transfer');
  }

  onTransferClosed(): void {
    this.transferSeed = null;
    this.isBalanceDecreasing = false;
  }

  onTransferCompleted(data: TransferCompletedData): void {
    this.transferCompletedData = data;
    void this.loadUserAccounts();
  }

  onReturnToFavoriteDetails(): void {
    this.transferSeed = null;
    this.isBalanceDecreasing = false;
    this.openModal('favoriteDetails');
  }

  onFavoriteTransferRequested(favorite: FavoriteContact): void {
    this.transferSeed = {
      destination: this.favoriteService.createTransferDataFromFavorite(favorite),
      step: 3,
      fromFavorite: true,
      preferredCurrency: this.activeCurrency,
    };
    this.closeAllModals();
    this.openModal('transfer');
  }

  onFavoriteTransferFlowFinished(): void {
    this.transferSeed = null;
    this.closeAllModals();
    this.isBalanceDecreasing = false;
  }

  openAliasModal(): void {
    this.openModal('alias');
  }

  closeAliasModal(): void {
    this.closeAllModals();
  }

  openTaxModal(): void {
    this.openModal('tax');
  }

  openMyQrModal(): void {
    this.openModal('myQr');
  }

  openProfileModal(): void {
    this.openModal('profile');
  }

  async openFavoritesModal(): Promise<void> {
    try {
      await this.favoriteService.loadFavoriteContacts();
      this.openModal('favorites');
    } catch {
      this.toast.show('Error al cargar contactos favoritos', 'error');
    }
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  toggleBalance(): void {
    this.balanceVisible = !this.balanceVisible;
  }

  logout(): void {
    this.isLoading = true;

    if (!this.sessionStore.hasAccessToken()) {
      setTimeout(() => this.performLocalLogout(), 1500);
      return;
    }

    this.authService.logoutUser().subscribe({
      next: () => setTimeout(() => this.performLocalLogout(), 1500),
      error: () => setTimeout(() => this.performLocalLogout(), 1500),
    });
  }

  private performLocalLogout(): void {
    this.authService.clearLocalSession();
    this.userData = {
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
    this.toast.show('Sesión cerrada exitosamente', 'success');
    this.isLoading = false;
    this.router.navigate(['/login'], { replaceUrl: true });
  }

  formatMoney(amount: number): string {
    return formatMoneyShared(amount);
  }

  onModalBackdropClick(event: MouseEvent, _modalType: string): void {
    if ((event.target as HTMLElement).classList.contains('modal')) {
      this.closeAllModals();
    }
  }

  async copyToClipboard(text: string, type: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(text);
      this.toast.show(`${type} copiado al portapapeles`, 'success');
    } catch (error) {
      logger.error('Error copying to clipboard:', error);
      this.toast.show(`No se pudo copiar el ${type}`, 'error');
    }
  }
}
