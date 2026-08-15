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
import { firstValueFrom } from 'rxjs';
import {
  TransferFlowService,
  TransferFlowError,
} from '../../../../services/transfer-flow/transfer-flow.service';
import { ToastService } from '../../../../services/toast/toast.service';
import { UserDataStore } from '../../../../services/user-data-store/user-data.store';
import { TransactionService } from '../../../../services/transaction/transaction.service';
import { ModalService } from '../../../../services/modal/modal.service';
import { ReceiptPdfService } from '../../../../services/receipt-pdf/receipt-pdf.service';
import { TransferData } from '../../../../models/transfer.interface';
import Transaction from '../../../../models/transaction';
import { UserAccount } from '../../../../services/account/account.service';
import { errorMessage } from '../../../../shared/utils/error-message';
import { formatMoney } from '../../../../shared/utils/money-format';
import { formatDni as formatDniAr } from '../../../../shared/utils/dni-format';
import qrData from '../../../../models/qrData';
import { logger } from '../../../../shared/utils/logger';

/** Balance (+ id opcional) para el wizard; usd-account puede pasar un view parcial. */
export type TransferSourceAccount =
  | Pick<UserAccount, 'balance'>
  | Pick<UserAccount, 'id' | 'balance'>
  | UserAccount;

export interface TransferWizardSeed {
  destination?: TransferData | null;
  step?: number;
  fromFavorite?: boolean;
  preferredCurrency?: 'ARS' | 'USD';
}

type TransferUiPhase = 'flow' | 'sending' | 'sent';

@Component({
  selector: 'app-transfer-wizard',
  standalone: true,
  imports: [CommonModule, FormsModule, ZXingScannerModule],
  templateUrl: './transfer-wizard.html',
  styleUrls: ['./transfer-wizard.css'],
})
export class TransferWizardComponent implements OnInit {
  @Input() arsAccount: TransferSourceAccount | null = null;
  @Input() usdAccount: TransferSourceAccount | null = null;
  /** IDs propias a rechazar (usd-account). Default: perfil actual. */
  @Input() ownAccountIds: Array<string | number | null | undefined> | null = null;
  @Input() showQrScan = true;
  @Input() showPostTransferFavorite = true;
  /** Si true, cierra vía ModalService (dashboard). Si false, solo emite closed (usd overlay). */
  @Input() useModalService = true;
  @Input() seed: TransferWizardSeed | null = null;
  /** Moneda preferida al abrir (p. ej. toggle del dashboard). */
  @Input() preferredCurrency: 'ARS' | 'USD' | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() transferCompleted = new EventEmitter<TransferData & { idaccount: number }>();
  @Output() accountsNeedReload = new EventEmitter<void>();
  @Output() balanceDecreasingChange = new EventEmitter<boolean>();
  @Output() openAddFavorite = new EventEmitter<void>();
  @Output() skipAddFavorite = new EventEmitter<void>();
  @Output() returnToFavoriteDetails = new EventEmitter<void>();

  transferStep = 1;
  uiPhase: TransferUiPhase = 'flow';
  isBuscandoCuenta = false;
  isScanning = false;
  hasPermission: boolean | null = null;
  destinatarioInput = '';
  cuentaDestinoData: TransferData | null = null;
  transferCurrency: 'ARS' | 'USD' = 'ARS';
  montoTransfer: number | null = null;
  isTransfiriendo = false;
  receiptBusy = false;
  private fromFavorite = false;
  private alreadyFavoriteAfterTransfer = false;
  private completedReceipt: Transaction | null = null;
  private readonly minSendingMs = 2000;

  constructor(
    private transferFlow: TransferFlowService,
    private toast: ToastService,
    private userDataStore: UserDataStore,
    private transactionService: TransactionService,
    private modalService: ModalService,
    private receiptPdf: ReceiptPdfService
  ) {}

  ngOnInit(): void {
    const preferred = this.seed?.preferredCurrency ?? this.preferredCurrency;
    if (preferred === 'USD' && this.usdAccount) {
      this.transferCurrency = 'USD';
    } else if (preferred === 'ARS' && this.arsAccount) {
      this.transferCurrency = 'ARS';
    } else {
      this.transferCurrency = this.arsAccount ? 'ARS' : 'USD';
    }

    if (this.seed?.destination) {
      this.cuentaDestinoData = this.seed.destination;
      this.transferStep = this.seed.step ?? 3;
      this.fromFavorite = !!this.seed.fromFavorite;
    }
  }

  get currencySymbol(): string {
    return this.transferCurrency === 'USD' ? 'U$S' : '$';
  }

  get formattedAmount(): string {
    if (!this.montoTransfer || this.montoTransfer <= 0) {
      return '0,00';
    }
    return formatMoney(this.montoTransfer);
  }

  get recipientName(): string {
    if (!this.cuentaDestinoData) {
      return '';
    }
    const { nombre, apellido } = this.cuentaDestinoData.user;
    return `${nombre || ''} ${apellido || ''}`.trim() || this.cuentaDestinoData.alias;
  }

  get canSearch(): boolean {
    return !!this.destinatarioInput.trim() && !this.isBuscandoCuenta;
  }

  get isOwnDestinationAccount(): boolean {
    if (!this.cuentaDestinoData) return false;
    const destId = String(this.cuentaDestinoData.idaccount);

    const arsId = this.arsAccount && 'id' in this.arsAccount && this.arsAccount.id != null
      ? String(this.arsAccount.id)
      : null;
    const usdId = this.usdAccount && 'id' in this.usdAccount && this.usdAccount.id != null
      ? String(this.usdAccount.id)
      : null;

    return destId === arsId || destId === usdId;
  }

  get isCrossCurrency(): boolean {
    return !!this.cuentaDestinoData && this.transferCurrency !== this.cuentaDestinoData.currency;
  }

  get canTransfer(): boolean {
    return (
      !!this.montoTransfer &&
      this.montoTransfer > 0 &&
      !this.isTransfiriendo &&
      this.uiPhase === 'flow' &&
      (!this.isCrossCurrency || this.isOwnDestinationAccount)
    );
  }

  get completedReceiptReady(): boolean {
    return !!this.completedReceipt;
  }

  get stepLabel(): string {
    switch (this.transferStep) {
      case 1:
        return 'Destinatario';
      case 2:
        return 'Confirmar';
      case 3:
        return 'Monto';
      case 4:
        return 'Listo';
      default:
        return '';
    }
  }

  formatBalance(amount: number): string {
    return formatMoney(amount);
  }

  formatDni(dni: string | number | null | undefined): string {
    return formatDniAr(dni);
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

  getSelectedAccountId(): string | null {
    const account =
      this.transferCurrency === 'USD' ? this.usdAccount : this.arsAccount;
    if (account && 'id' in account && account.id != null && account.id !== '') {
      return String(account.id);
    }
    return null;
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
    } catch (error: unknown) {
      logger.error('Error buscando cuenta:', error);
      const message =
        error instanceof TransferFlowError
          ? error.code === 'SELF_TRANSFER' && !this.showPostTransferFavorite
            ? 'No puedes transferir a tu misma cuenta'
            : error.message
          : errorMessage(error, 'Cuenta no encontrada');
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
    } catch (error: unknown) {
      if (error instanceof TransferFlowError) {
        const message =
          !this.showPostTransferFavorite && error.code === 'INSUFFICIENT_FUNDS'
            ? 'Saldo insuficiente'
            : error.message;
        this.toast.show(message, 'error');
      }
      return;
    }

    if (!this.cuentaDestinoData) {
      this.toast.show('Destinatario no seleccionado', 'error');
      return;
    }

    this.isTransfiriendo = true;
    this.uiPhase = 'sending';
    this.balanceDecreasingChange.emit(true);
    const startedAt = Date.now();

    try {
      const result = await this.transferFlow.executeTransfer({
        destination: this.cuentaDestinoData,
        amount: this.montoTransfer,
        balance: this.getSelectedAccountBalance(),
        currency: this.transferCurrency,
        originAccountId: this.getSelectedAccountId(),
      });

      await Promise.all([
        firstValueFrom(this.userDataStore.load(true)),
        this.transactionService.loadAllTransactions(true).catch(() => undefined),
      ]);
      this.accountsNeedReload.emit();

      const elapsed = Date.now() - startedAt;
      if (elapsed < this.minSendingMs) {
        await this.wait(this.minSendingMs - elapsed);
      }

      this.transferCompleted.emit(result.completedData);
      this.alreadyFavoriteAfterTransfer = result.alreadyFavorite;
      this.completedReceipt = this.buildReceiptTransaction(result.completedData);
      this.uiPhase = 'sent';
      this.balanceDecreasingChange.emit(false);
    } catch (error: unknown) {
      logger.error('Error realizando transferencia:', error);
      this.balanceDecreasingChange.emit(false);
      this.uiPhase = 'flow';
      const message =
        error instanceof TransferFlowError
          ? error.message
          : errorMessage(error, 'Error al realizar la transferencia');
      this.toast.show(message, 'error');
      if (!(error instanceof TransferFlowError) || error.code === 'TRANSFER_FAILED') {
        this.userDataStore.load(true).subscribe();
      }
    } finally {
      this.isTransfiriendo = false;
    }
  }

  async downloadReceipt(): Promise<void> {
    if (!this.completedReceipt || this.receiptBusy) return;
    this.receiptBusy = true;
    try {
      await this.receiptPdf.download(this.completedReceipt, this.receiptLabels());
      this.toast.show('Comprobante descargado', 'success');
    } catch (error) {
      logger.error('Error generando PDF', error);
      this.toast.show('No se pudo generar el comprobante', 'error');
    } finally {
      this.receiptBusy = false;
    }
  }

  async shareReceipt(): Promise<void> {
    if (!this.completedReceipt || this.receiptBusy) return;
    this.receiptBusy = true;
    try {
      const result = await this.receiptPdf.share(this.completedReceipt, this.receiptLabels());
      this.toast.show(
        result === 'shared'
          ? 'Comprobante listo para compartir'
          : 'Comprobante descargado para que lo compartas',
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

  /** Continuar después del éxito: favoritos o cerrar. */
  continueAfterSent(): void {
    if (this.receiptBusy) return;
    if (this.showPostTransferFavorite && !this.alreadyFavoriteAfterTransfer) {
      this.uiPhase = 'flow';
      this.transferStep = 4;
      return;
    }
    this.toast.show('Transferencia realizada con éxito', 'success');
    this.closeInternal(true);
  }

  private receiptLabels() {
    return {
      title: 'Detalle de transferencia',
      description: this.completedReceipt?.description || 'Transferencia',
      amountLabel: `-${this.currencySymbol} ${this.formattedAmount}`,
      origin: 'Tu cuenta',
      destination: this.recipientName || this.cuentaDestinoData?.alias || 'Destinatario',
    };
  }

  private buildReceiptTransaction(
    completed: TransferData & { idaccount: number }
  ): Transaction {
    const amount = this.montoTransfer || 0;
    const name = this.recipientName || completed.alias;
    const recent = this.transactionService.getRecentTransactions();
    const match = recent.find(
      (tx) =>
        tx.type === 'expense' &&
        tx.kind === 'transfer' &&
        Math.abs(tx.amount - amount) < 0.01 &&
        (tx.currency || this.transferCurrency) === this.transferCurrency
    );

    return {
      id: match?.id ?? Date.now(),
      type: 'expense',
      kind: 'transfer',
      description: `Enviaste a ${name}`,
      amount,
      date: match?.date ?? new Date(),
      to: name,
      counterpartyName: name,
      status: 'COMPLETED',
      currency: this.transferCurrency,
      idOperation: match?.idOperation,
      destinationId: completed.idaccount,
    };
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
    logger.error('Error con el escáner:', error);
    this.toast.show('Error al iniciar la cámara', 'error');
  }

  handleScanSuccess(resultString: string): void {
    this.isScanning = false;
    this.isBuscandoCuenta = true;

    setTimeout(() => {
      try {
        const qrDataPayload = JSON.parse(resultString) as qrData;

        if (qrDataPayload && qrDataPayload.walletApp === 'ArCashV1') {
          try {
            this.transferFlow.assertNotSelf(
              qrDataPayload.accountId,
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

          const nameParts = (qrDataPayload.receiverName || '').trim().split(/\s+/);
          this.cuentaDestinoData = {
            alias: qrDataPayload.accountAlias,
            cvu: 'Obtenido por QR',
            currency: qrDataPayload.currency === 'USD' ? 'USD' : 'ARS',
            user: {
              nombre: nameParts[0] || '',
              apellido: nameParts.slice(1).join(' ') || '',
              dni: qrDataPayload.dni,
            },
            idaccount: qrDataPayload.accountId,
          };
          this.transferStep = 2;
        } else {
          throw new Error('QR no válido para ArCash');
        }
      } catch (error) {
        logger.error('Error al procesar QR:', error);
        this.toast.show('El código QR no es válido', 'error');
      } finally {
        this.isBuscandoCuenta = false;
      }
    }, 500);
  }

  close(): void {
    if (this.isTransfiriendo || this.uiPhase === 'sending') {
      return;
    }
    if (this.uiPhase === 'sent') {
      this.continueAfterSent();
      return;
    }
    if (this.fromFavorite) {
      this.resetState();
      // Volver al detalle sin pasar por closeModal(null), que borraba el scope.
      this.returnToFavoriteDetails.emit();
      return;
    }
    this.closeInternal(true);
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('transfer-modal')) {
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
    this.uiPhase = 'flow';
    this.destinatarioInput = '';
    this.montoTransfer = null;
    this.cuentaDestinoData = null;
    this.isBuscandoCuenta = false;
    this.isTransfiriendo = false;
    this.isScanning = false;
    this.fromFavorite = false;
    this.alreadyFavoriteAfterTransfer = false;
    this.completedReceipt = null;
    this.receiptBusy = false;
    this.balanceDecreasingChange.emit(false);
  }

  private wait(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }
}
