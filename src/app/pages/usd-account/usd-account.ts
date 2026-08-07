import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription, interval, forkJoin, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';

// Services
import { AccountService } from '../../services/account-service/account.service';
import { DataService } from '../../services/data-service/data-service';
import { UtilService } from '../../services/util-service/util-service';
import { TransactionService } from '../../services/transaction-service/transaction-service';
import { ModalService } from '../../services/modal-service/modal-service';

// Models
import Transaction from '../../models/transaction';
import UserData from '../../models/user-data';

@Component({
  selector: 'app-usd-account',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './usd-account.html',
  styleUrls: ['./usd-account.css']
})
export class UsdAccountComponent implements OnInit, OnDestroy {
  
  private subscriptions: Subscription[] = [];
  private dataPollingSubscription: Subscription | null = null;
  
  // Estados de carga
  isLoading = true;
  isCreatingUsdAccount = false;
  isBuyingUsd = false;
  isSellingUsd = false;
  isTransferring = false;
  isSearchingAccount = false;
  
  // Datos de cuentas
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
  
  // Transacciones USD
  usdTransactions: Transaction[] = [];
  filteredTransactions: Transaction[] = [];
  selectedFilter: 'ALL' | 'ARS' | 'USD' = 'ALL';
  selectedTransaction: Transaction | null = null;
  showTransactionDetail = false;
  
  // Estados de modales/secciones
  showBuyUsdSection = false;
  showSellUsdSection = false;
  showTransferSection = false;
  showReceiveSection = false;
  
  // Datos de operaciones
  amountToBuyUsd: number | null = null;
  amountToSellUsd: number | null = null;
  amountToTransfer: number | null = null;
  selectedCurrency: 'ARS' | 'USD' = 'USD';
  
  // Conversión en tiempo real para compra USD
  estimatedUsdAmount: number = 0;
  estimatedArsAmount: number = 0;
  currentExchangeRate: number = 0;
  taxPercentage: number = 3; // Comisión del servicio
  
  // Transfer data
  transferStep = 1;
  destinationInput = '';
  destinationAccountData: any = null;
  
  // Exchange rate
  exchangeRate = 0;

  constructor(
    private router: Router,
    private accountService: AccountService,
    private dataService: DataService,
    private utilService: UtilService,
    private transactionService: TransactionService,
    private modalService: ModalService
  ) {}

  ngOnInit(): void {
    this.loadUserData();
    this.checkUsdAccount();
    this.loadExchangeRate();
    this.startDataPolling(10000); // Iniciar polling cada 10 segundos
  }

  ngOnDestroy(): void {
    this.stopDataPolling();
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private loadUserData(): void {
    const userDataSub = this.dataService.userData$.subscribe(userData => {
      if (userData) {
        this.userData = userData;
        this.arsBalance = userData.balance;
        this.arsAccountId = userData.idAccount;
      }
      this.isLoading = false;
    });
    this.subscriptions.push(userDataSub);
    
    this.dataService.loadUserData(true).subscribe();
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

  // =================== CREAR CUENTA USD ===================
  
  async createUsdAccount(): Promise<void> {
    this.isCreatingUsdAccount = true;
    
    try {
      const response = await this.accountService.openUsdAccount().toPromise();
      
      if (response.success) {
        this.utilService.showToast('Cuenta en dólares creada exitosamente', 'success');
        this.usdAccountId = response.accountId;
        this.hasUsdAccount = true;
        this.usdBalance = 0;
        // Recargar las cuentas para actualizar la información
        this.checkUsdAccount();
      } else {
        this.utilService.showToast(response.message || 'Error al crear cuenta en dólares', 'error');
      }
    } catch (error: any) {
      console.error('Error creando cuenta USD:', error);
      this.utilService.showToast(error.error?.message || 'Error al crear cuenta en dólares', 'error');
    } finally {
      this.isCreatingUsdAccount = false;
    }
  }

  // =================== COMPRAR DÓLARES ===================
  
  openBuyUsdSection(): void {
    this.closeSections();
    this.showBuyUsdSection = true;
  }

  closeBuyUsdSection(): void {
    this.showBuyUsdSection = false;
    this.amountToBuyUsd = null;
    this.estimatedUsdAmount = 0;
  }

  async buyUsd(): Promise<void> {
    if (!this.amountToBuyUsd || this.amountToBuyUsd <= 0) {
      this.utilService.showToast('Por favor ingrese un monto válido', 'error');
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
        // Guardar el exchange rate para futuras conversiones
        if (response.exchangeRate) {
          this.currentExchangeRate = response.exchangeRate;
        }
        
        this.utilService.showToast(
          `Compra exitosa: $${response.amountUsd.toFixed(2)} USD`,
          'success'
        );
        
        this.arsBalance = response.newBalanceArs;
        this.usdBalance = response.newBalanceUsd;
        this.userData.balance = response.newBalanceArs;
        
        this.closeBuyUsdSection();
        this.loadTransactions();
        this.dataService.loadUserData(true).subscribe();
      } else {
        this.utilService.showToast(response?.message || 'Error en la compra', 'error');
      }
    } catch (error: any) {
      console.error('Error comprando USD:', error);
      this.utilService.showToast(
        error.error?.message || 'Error al comprar dólares',
        'error'
      );
    } finally {
      this.isBuyingUsd = false;
    }
  }

  // =================== VENDER DÓLARES ===================
  
  openSellUsdSection(): void {
    this.closeSections();
    this.showSellUsdSection = true;
  }

  closeSellUsdSection(): void {
    this.showSellUsdSection = false;
    this.amountToSellUsd = null;
    this.estimatedArsAmount = 0;
  }

  async sellUsd(): Promise<void> {
    if (!this.amountToSellUsd || this.amountToSellUsd <= 0) {
      this.utilService.showToast('Por favor ingrese un monto válido', 'error');
      return;
    }

    if (this.amountToSellUsd > this.usdBalance) {
      this.utilService.showToast('Saldo insuficiente en dólares', 'error');
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
        // Guardar el exchange rate para futuras conversiones
        if (response.exchangeRate) {
          this.currentExchangeRate = response.exchangeRate;
        }
        
        this.utilService.showToast(
          `Venta exitosa: $${response.amountArs.toFixed(2)} ARS`,
          'success'
        );
        
        this.usdBalance = response.newBalanceUsd;
        this.arsBalance = response.newBalanceArs;
        this.userData.balance = response.newBalanceArs;
        
        this.closeSellUsdSection();
        this.loadTransactions();
        this.dataService.loadUserData(true).subscribe();
      } else {
        this.utilService.showToast(response?.message || 'Error en la venta', 'error');
      }
    } catch (error: any) {
      console.error('Error vendiendo USD:', error);
      this.utilService.showToast(
        error.error?.message || 'Error al vender dólares',
        'error'
      );
    } finally {
      this.isSellingUsd = false;
    }
  }

  // =================== TRANSFERIR ===================
  
  openTransferSection(): void {
    this.closeSections();
    this.showTransferSection = true;
    this.transferStep = 1;
  }

  closeTransferSection(): void {
    this.showTransferSection = false;
    this.transferStep = 1;
    this.destinationInput = '';
    this.destinationAccountData = null;
    this.amountToTransfer = null;
  }

  async searchDestinationAccount(): Promise<void> {
    if (!this.destinationInput.trim()) {
      this.utilService.showToast('Por favor ingrese un Alias o CVU', 'error');
      return;
    }

    this.isSearchingAccount = true;

    try {
      const accountData = await this.dataService.buscarCuenta(this.destinationInput.trim());
      
      // Verificar que no sea la misma cuenta
      if (this.selectedCurrency === 'ARS' && accountData.idaccount === this.arsAccountId) {
        this.utilService.showToast('No puedes transferir a tu misma cuenta', 'error');
        this.isSearchingAccount = false;
        return;
      }
      
      if (this.selectedCurrency === 'USD' && accountData.idaccount === this.usdAccountId) {
        this.utilService.showToast('No puedes transferir a tu misma cuenta', 'error');
        this.isSearchingAccount = false;
        return;
      }
      
      this.destinationAccountData = accountData;
      this.transferStep = 2;
      
    } catch (error: any) {
      console.error('Error buscando cuenta:', error);
      this.utilService.showToast(error.message || 'Cuenta no encontrada', 'error');
    } finally {
      this.isSearchingAccount = false;
    }
  }

  confirmDestinationAccount(): void {
    this.transferStep = 3;
  }

  cancelSearch(): void {
    this.transferStep = 1;
    this.destinationAccountData = null;
  }

  async executeTransfer(): Promise<void> {
    if (!this.amountToTransfer || this.amountToTransfer <= 0) {
      this.utilService.showToast('Por favor ingrese un monto válido', 'error');
      return;
    }

    const currentBalance = this.selectedCurrency === 'ARS' ? this.arsBalance : this.usdBalance;
    
    if (this.amountToTransfer > currentBalance) {
      this.utilService.showToast('Saldo insuficiente', 'error');
      return;
    }

    this.isTransferring = true;

    try {
      const sourceAccountId = this.selectedCurrency === 'ARS' ? this.arsAccountId : this.usdAccountId;
      
      await this.dataService.realizarTransferencia(
        this.destinationAccountData.idaccount.toString(),
        this.amountToTransfer,
        this.selectedCurrency
      );
      
      this.utilService.showToast('Transferencia realizada con éxito', 'success');
      
      // Recargar datos
      this.dataService.loadUserData(true).subscribe();
      this.loadTransactions();
      this.closeTransferSection();
      
    } catch (error: any) {
      console.error('Error realizando transferencia:', error);
      this.utilService.showToast('Error al realizar la transferencia', 'error');
    } finally {
      this.isTransferring = false;
    }
  }

  // =================== RECIBIR ===================
  
  openReceiveSection(): void {
    this.closeSections();
    this.showReceiveSection = true;
  }

  closeReceiveSection(): void {
    this.showReceiveSection = false;
  }

  copyToClipboard(text: string, label: string): void {
    navigator.clipboard.writeText(text).then(() => {
      this.utilService.showToast(`${label} copiado al portapapeles`, 'success');
    }).catch(() => {
      this.utilService.showToast('Error al copiar', 'error');
    });
  }

  // =================== HELPERS ===================
  
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
        // Filtrar transacciones USD (basado en descripción o campo currency)
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
    this.stopDataPolling(); // Evita duplicados

    this.dataPollingSubscription = interval(intervalMs)
      .pipe(
        switchMap(() => {
          // Actualizar datos del usuario ARS y cuenta USD
          return forkJoin([
            this.dataService.loadUserData(true),
            this.hasUsdAccount ? this.accountService.getUserAccounts() : of(null),
            this.hasUsdAccount ? this.transactionService.loadAllTransactions(true) : of(null)
          ]);
        })
      )
      .subscribe({
        next: ([userData, accounts]) => {
          // Actualizar saldo USD si existe la cuenta
          if (accounts) {
            const usdAccount = accounts.find(acc => acc.currency === 'USD');
            if (usdAccount) {
              this.usdBalance = usdAccount.balance;
              this.usdAccountId = usdAccount.id;
              this.usdAlias = usdAccount.alias;
              this.usdCvu = usdAccount.cvu;
            }
          }
        },
        error: (err) => {
          console.error('>>> Polling: Error durante la actualización de datos USD:', err);
        }
      });

    // Guardamos la suscripción para poder cancelarla en ngOnDestroy
    if (this.dataPollingSubscription) {
      this.subscriptions.push(this.dataPollingSubscription);
    }
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
    if (amount == null || isNaN(amount)) return '0';
    if (amount % 1 === 0) {
      return amount.toLocaleString('es-AR');
    }
    const formatted = amount.toLocaleString('es-AR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2
    });
    return formatted;
  }

  formatDate(date: Date): string {
    return this.transactionService.formatDate(date);
  }

  getCurrencyLabel(currency: string): string {
    return currency === 'ARS' ? 'Pesos' : 'Dólares';
  }

  // =================== CONVERSIÓN EN TIEMPO REAL ===================
  
  private loadExchangeRate(): void {
    // Obtener el tipo de cambio actual
    this.accountService.getUserAccounts().subscribe({
      next: () => {
        // El tipo de cambio se actualizará cuando sea necesario
        this.currentExchangeRate = 1100; // Valor por defecto, se actualizará con la API
      },
      error: (error) => {
        console.error('Error cargando tipo de cambio:', error);
        this.currentExchangeRate = 1100; // Valor fallback
      }
    });
  }

  onAmountToBuyChange(): void {
    if (this.amountToBuyUsd && this.amountToBuyUsd > 0) {
      // El usuario ingresa ARS que quiere gastar
      // Calcular el tipo de cambio efectivo con impuestos (65% = 30% PAIS + 35% Ganancias)
      const effectiveRate = this.currentExchangeRate * (1 + this.taxPercentage / 100);
      // Estimar los USD que se obtendrían
      this.estimatedUsdAmount = this.amountToBuyUsd / effectiveRate;
    } else {
      this.estimatedUsdAmount = 0;
    }
  }

  onAmountToSellChange(): void {
    if (this.amountToSellUsd && this.amountToSellUsd > 0) {
      // Calcular los ARS que se obtendrían
      this.estimatedArsAmount = this.amountToSellUsd * this.currentExchangeRate;
    } else {
      this.estimatedArsAmount = 0;
    }
  }

  // =================== FILTROS Y DETALLES ===================
  
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
    // Solo mostrar en USD si es una transferencia directa USD → USD
    // (mismo currency en origen y destino)
    if (transaction.currency === 'USD' && transaction.originalCurrency === 'USD') {
      return transaction.amount;
    }
    
    // Para todas las otras combinaciones, mostrar en ARS
    if (transaction.amountInArs) {
      return transaction.amountInArs;
    }
    
    return transaction.amount;
  }

  getDisplayCurrency(transaction: Transaction): string {
    // Solo mostrar USD si es una transferencia directa USD → USD
    if (transaction.currency === 'USD' && transaction.originalCurrency === 'USD') {
      return 'USD';
    }
    
    // En todos los otros casos, mostrar ARS
    return 'ARS';
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
