import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ZXingScannerModule } from '@zxing/ngx-scanner';
import {
  TransferFlowService,
  TransferFlowError,
} from '../../../../services/transfer-flow/transfer-flow.service';
import { ToastService } from '../../../../services/toast-service/toast.service';
import { UserDataStore } from '../../../../services/user-data-store/user-data.store';
import { TransactionService } from '../../../../services/transaction-service/transaction-service';
import { ModalService } from '../../../../services/modal-service/modal-service';
import { TransferData } from '../../../../models/transfer.interface';

export interface TransferWizardSeed {
  destination?: TransferData | null;
  step?: number;
  fromFavorite?: boolean;
}

@Component({
  selector: 'app-transfer-wizard',
  standalone: true,
  imports: [CommonModule, FormsModule, ZXingScannerModule],
  templateUrl: './transfer-wizard.html',
  styleUrls: [
    '../../styles/modals-shared.css',
    '../../currency-selector.css',
    '../../styles/transfer-wizard.css',
  ],
})
export class TransferWizardComponent implements OnInit {
  @Input() arsAccount: any = null;
  @Input() usdAccount: any = null;
  /** IDs propias a rechazar (usd-account). Default: perfil actual. */
  @Input() ownAccountIds: Array<string | number | null | undefined> | null = null;
  @Input() showQrScan = true;
  @Input() showPostTransferFavorite = true;
  /** Si true, cierra vía ModalService (dashboard). Si false, solo emite closed (usd overlay). */
  @Input() useModalService = true;
  @Input() seed: TransferWizardSeed | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() transferCompleted = new EventEmitter<TransferData & { idaccount: number }>();
  @Output() accountsNeedReload = new EventEmitter<void>();
  @Output() balanceDecreasingChange = new EventEmitter<boolean>();
  @Output() openAddFavorite = new EventEmitter<void>();
  @Output() skipAddFavorite = new EventEmitter<void>();
  @Output() returnToFavoriteDetails = new EventEmitter<void>();

  transferStep = 1;
  isBuscandoCuenta = false;
  isScanning = false;
  hasPermission: boolean | null = null;
  destinatarioInput = '';
  cuentaDestinoData: any = null;
  transferCurrency: 'ARS' | 'USD' = 'ARS';
  montoTransfer: number | null = null;
  isTransfiriendo = false;
  private fromFavorite = false;

  constructor(
    private transferFlow: TransferFlowService,
    private toast: ToastService,
    private userDataStore: UserDataStore,
    private transactionService: TransactionService,
    private modalService: ModalService
  ) {}

  ngOnInit(): void {
    this.transferCurrency = this.arsAccount ? 'ARS' : 'USD';
    if (this.seed?.destination) {
      this.cuentaDestinoData = this.seed.destination;
      this.transferStep = this.seed.step ?? 3;
      this.fromFavorite = !!this.seed.fromFavorite;
    }
  }

  getSelectedAccountBalance(): number {
    if (this.transferCurrency === 'USD' && this.usdAccount) {
      return this.usdAccount.balance;
    }
    if (this.transferCurrency === 'ARS' && this.arsAccount) {
      return this.arsAccount.balance;
    }
    return 0;
  }

  onCurrencySelected(currency: 'ARS' | 'USD'): void {
    this.transferCurrency = currency;
  }

  async buscarCuenta(): Promise<void> {
    this.isBuscandoCuenta = true;
    try {
      this.cuentaDestinoData = await this.transferFlow.searchDestination(
        this.destinatarioInput,
        {
          ownAccountIds: this.ownAccountIds ?? undefined,
          checkFavorites: this.showPostTransferFavorite,
        }
      );
      this.transferStep = 2;
    } catch (error: any) {
      console.error('Error buscando cuenta:', error);
      const message =
        error instanceof TransferFlowError
          ? error.code === 'SELF_TRANSFER' && !this.showPostTransferFavorite
            ? 'No puedes transferir a tu misma cuenta'
            : error.message
          : error?.message || 'Cuenta no encontrada';
      this.toast.show(message, 'error');
    } finally {
      this.isBuscandoCuenta = false;
    }
  }

  confirmarCuenta(): void {
    this.transferStep = 3;
  }

  cancelarBusqueda(): void {
    if (this.fromFavorite) {
      this.resetState();
      this.returnToFavoriteDetails.emit();
      this.closeInternal(false);
      return;
    }
    this.transferStep = 1;
    this.destinatarioInput = '';
    this.cuentaDestinoData = null;
    this.isScanning = false;
  }

  volverAConfirmacion(): void {
    if (this.fromFavorite) {
      this.resetState();
      this.returnToFavoriteDetails.emit();
      this.closeInternal(false);
      return;
    }
    this.transferStep = 2;
    this.montoTransfer = null;
  }

  async realizarTransferencia(): Promise<void> {
    const fundsCurrency = this.showPostTransferFavorite
      ? this.transferCurrency
      : undefined;

    try {
      this.transferFlow.validateAmount(
        this.montoTransfer,
        this.getSelectedAccountBalance(),
        fundsCurrency
      );
    } catch (error: any) {
      if (error instanceof TransferFlowError) {
        const message =
          !this.showPostTransferFavorite && error.code === 'INSUFFICIENT_FUNDS'
            ? 'Saldo insuficiente'
            : error.message;
        this.toast.show(message, 'error');
      }
      return;
    }

    if (this.showPostTransferFavorite) {
      setTimeout(() => this.balanceDecreasingChange.emit(true), 7800);
    }

    this.isTransfiriendo = true;

    try {
      const result = await this.transferFlow.executeTransfer({
        destination: this.cuentaDestinoData,
        amount: this.montoTransfer,
        balance: this.getSelectedAccountBalance(),
        currency: this.transferCurrency,
      });

      this.userDataStore.load(true).subscribe();
      this.accountsNeedReload.emit();

      if (this.showPostTransferFavorite) {
        setTimeout(() => this.balanceDecreasingChange.emit(false), 9300);
      }

      this.transferCompleted.emit(result.completedData);

      if (this.showPostTransferFavorite) {
        if (result.alreadyFavorite) {
          this.toast.show('Transferencia realizada con éxito', 'success');
          this.close();
        } else {
          this.transferStep = 4;
        }
        await this.transactionService.loadAllTransactions(true);
      } else {
        this.toast.show('Transferencia realizada con éxito', 'success');
        this.close();
      }
    } catch (error: any) {
      console.error('Error realizando transferencia:', error);
      this.balanceDecreasingChange.emit(false);
      const message =
        error instanceof TransferFlowError
          ? error.message
          : 'Error al realizar la transferencia';
      this.toast.show(message, 'error');
      if (!(error instanceof TransferFlowError) || error.code === 'TRANSFER_FAILED') {
        this.userDataStore.load(true).subscribe();
      }
    } finally {
      this.isTransfiriendo = false;
    }
  }

  startScanning(): void {
    this.isScanning = true;
    this.hasPermission = null;
  }

  cancelScanning(): void {
    this.isScanning = false;
  }

  handlePermissionResponse(permission: boolean): void {
    this.hasPermission = permission;
    if (!permission) {
      this.toast.show('Permiso de cámara denegado', 'error');
      this.isScanning = false;
    }
  }

  handleScanError(error: Error): void {
    console.error('Error con el escáner:', error);
    this.toast.show('Error al iniciar la cámara', 'error');
  }

  handleScanSuccess(resultString: string): void {
    this.isScanning = false;
    this.isBuscandoCuenta = true;

    setTimeout(() => {
      try {
        const qrData = JSON.parse(resultString);

        if (qrData && qrData.walletApp === 'ArCashV1') {
          try {
            this.transferFlow.assertNotSelf(
              qrData.accountId,
              this.ownAccountIds ?? undefined
            );
          } catch {
            this.toast.show(
              'No puedes transferir a tu misma cuenta',
              'error'
            );
            this.isBuscandoCuenta = false;
            return;
          }

          this.cuentaDestinoData = {
            alias: qrData.accountAlias,
            cvu: 'Obtenido por QR',
            user: {
              nombre: qrData.receiverName.split(' ')[0],
              apellido: qrData.receiverName.split(' ').slice(1).join(' '),
              dni: qrData.dni,
              email: qrData.email,
            },
            idaccount: qrData.accountId,
          };
          this.transferStep = 2;
        } else {
          throw new Error('QR no válido para ArCash');
        }
      } catch (error) {
        console.error('Error al procesar QR:', error);
        this.toast.show('El código QR no es válido', 'error');
      } finally {
        this.isBuscandoCuenta = false;
      }
    }, 500);
  }

  close(): void {
    if (this.fromFavorite) {
      this.resetState();
      this.returnToFavoriteDetails.emit();
      this.closeInternal(false);
      return;
    }
    this.closeInternal(true);
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal')) {
      this.close();
    }
  }

  private closeInternal(emitClosed: boolean): void {
    if (this.useModalService) {
      this.modalService.closeModal();
    }
    this.resetState();
    if (emitClosed) {
      this.closed.emit();
    }
  }

  private resetState(): void {
    this.transferStep = 1;
    this.destinatarioInput = '';
    this.montoTransfer = null;
    this.cuentaDestinoData = null;
    this.isBuscandoCuenta = false;
    this.isTransfiriendo = false;
    this.isScanning = false;
    this.fromFavorite = false;
    this.balanceDecreasingChange.emit(false);
  }
}
