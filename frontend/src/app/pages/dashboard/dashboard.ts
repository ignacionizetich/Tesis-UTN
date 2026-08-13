import { Component, OnInit, OnDestroy, ChangeDetectorRef, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser, CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription, of, from } from 'rxjs';

import { themeService } from '../../services/theme-service/theme-service';
import { ToastService } from '../../services/toast-service/toast.service';
import { AuthService } from '../../services/auth-service/auth-service';
import { UserDataStore } from '../../services/user-data-store/user-data.store';
import { TransactionHistoryStore } from '../../services/transaction-history-store/transaction-history.store';
import { ModalService } from '../../services/modal-service/modal-service';
import { TransactionService } from '../../services/transaction-service/transaction-service';
import { FavoriteService } from '../../services/favorite-service/favorite-service';
import { AdminService } from '../../services/admin-service/admin.service';
import { AccountService } from '../../services/account-service/account.service';
import { SessionStore } from '../../core/session/session-store';
import { AccountPollingCoordinator } from '../../services/account-polling/account-polling.coordinator';
import { formatMoney as formatMoneyShared } from '../../shared/utils/money-format';

import { DepositModalComponent } from './components/deposit-modal/deposit-modal';
import {
  TransferWizardComponent,
  TransferWizardSeed,
} from './components/transfer-wizard/transfer-wizard';
import { AliasModalComponent } from './components/alias-modal/alias-modal';
import { TaxModalComponent } from './components/tax-modal/tax-modal';
import { ProfileModalComponent } from './components/profile-modal/profile-modal';
import { ReceiveQrModalComponent } from './components/receive-qr-modal/receive-qr-modal';
import { FavoritesModalsComponent } from './components/favorites-modals/favorites-modals';
import { TransactionsPanelComponent } from './components/transactions-panel/transactions-panel';

import UserData from '../../models/user-data';

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
    TransactionsPanelComponent,
  ],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css', './currency-selector.css'],
})
export class DashboardComponent implements OnInit, OnDestroy {
  private subscriptions: Subscription[] = [];
  private dataPollingSubscription: Subscription | null = null;

  isLoading = true;
  balanceVisible = true;
  isAdmin = false;

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

  transferCompletedData: any = null;
  transferSeed: TransferWizardSeed | null = null;

  userAccounts: any[] = [];
  arsAccount: any = null;
  usdAccount: any = null;

  isBalanceUpdating = false;
  isBalanceDecreasing = false;

  constructor(
    private router: Router,
    private themeService: themeService,
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

  ngOnInit(): void {
    this.checkAuthentication();
    this.checkAdminRole();
    this.setupSubscriptions();

    this.userDataStore.load(true).subscribe({
      next: (data) => {
        if (!data) {
          console.error('>>> Dashboard ngOnInit: loadUserData inicial devolvió null.');
        }
      },
      error: (err) =>
        console.error('>>> Dashboard ngOnInit: ERROR crítico en loadUserData inicial:', err),
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
        () => {
          if (this.modalService.getCurrentModal() === 'allTransactions') {
            return of(null);
          }
          return from(this.transactionService.loadAllTransactions(true));
        },
      ],
      {
        onError: (err) => {
          console.error('>>> Polling: Error durante la actualización de datos:', err);
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
        this.transactionService.loadAllTransactions(),
        this.favoriteService.loadFavoriteContacts(),
        this.loadUserAccounts(),
      ]);
    } catch (error) {
      console.error('Error inicializando services:', error);
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
      console.error('Error cargando cuentas del usuario:', error);
    }
  }

  private async loadDataInBackground(): Promise<void> {
    try {
      await Promise.allSettled([
        this.userDataStore.load(true),
        this.transactionHistoryStore.load(),
      ]);
    } catch (error) {
      console.error('❌ Error cargando datos:', error);
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
    } catch (error: any) {
      console.error('Error al verificar acceso de admin:', error);
      if (error.status === 403 || error.status === 401) {
        this.toast.show(
          'No tienes permisos para acceder al panel de administración',
          'error'
        );
      } else if (error.status === 0) {
        this.toast.show(
          'No se puede conectar con el servidor. Verifica que el backend esté ejecutándose.',
          'error'
        );
      } else {
        this.toast.show('Error del servidor. Intenta más tarde.', 'error');
      }
    } finally {
      this.isLoading = false;
    }
  }

  onDepositSuccess(): void {
    setTimeout(() => {
      this.isBalanceUpdating = true;
      setTimeout(() => {
        this.isBalanceUpdating = false;
      }, 1500);
    }, 100);
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

  openTransferModal(): void {
    this.transferSeed = null;
    this.isBalanceDecreasing = false;
    this.openModal('transfer');
  }

  onTransferClosed(): void {
    this.transferSeed = null;
    this.isBalanceDecreasing = false;
  }

  onTransferCompleted(data: any): void {
    this.transferCompletedData = data;
  }

  onReturnToFavoriteDetails(): void {
    this.transferSeed = null;
    this.isBalanceDecreasing = false;
    this.openModal('favoriteDetails');
  }

  onFavoriteTransferRequested(favorite: any): void {
    this.transferSeed = {
      destination: this.favoriteService.createTransferDataFromFavorite(favorite),
      step: 3,
      fromFavorite: true,
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
      console.error('Error copying to clipboard:', error);
      this.toast.show(`No se pudo copiar el ${type}`, 'error');
    }
  }

  goToUsdAccount(): void {
    this.router.navigate(['/usd-account']);
  }
}
