import {
  Component,
  EventEmitter,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { TransferApi } from '../../../../services/transfer-api/transfer.api';
import { ToastService } from '../../../../services/toast/toast.service';
import { ModalService } from '../../../../services/modal/modal.service';
import { UserDataStore } from '../../../../services/user-data-store/user-data.store';
import { AccountService } from '../../../../services/account/account.service';
import { formatMoney } from '../../../../shared/utils/money-format';
import { logger } from '../../../../shared/utils/logger';

type DepositPhase = 'form' | 'crediting' | 'success';

@Component({
  selector: 'app-deposit-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './deposit-modal.html',
  styleUrls: ['./deposit-modal.css'],
})
export class DepositModalComponent {
  readonly quickAmounts = [1000, 5000, 10000, 25000, 50000];
  /** Tiempo mínimo de la animación de acreditación (ms).
   * Cubrir ~1 ciclo de polling del dashboard para que el saldo ya esté fresco. */
  private readonly minCreditingMs = 2000;
  private readonly successHoldMs = 700;

  phase: DepositPhase = 'form';
  montoIngresar: number | null = null;
  isIngresandoDinero = false;
  creditedAmount = 0;

  @Output() closed = new EventEmitter<void>();
  /** Emitido tras depósito exitoso (para animar balance en el parent). */
  @Output() success = new EventEmitter<number>();
  @Output() backdropClick = new EventEmitter<MouseEvent>();

  constructor(
    private transferApi: TransferApi,
    private toast: ToastService,
    private modalService: ModalService,
    private userDataStore: UserDataStore,
    private accountService: AccountService
  ) {}

  get canSubmit(): boolean {
    return (
      this.phase === 'form' &&
      !!this.montoIngresar &&
      this.montoIngresar > 0 &&
      !this.isIngresandoDinero
    );
  }

  get formattedAmount(): string {
    if (!this.montoIngresar || this.montoIngresar <= 0) {
      return '0,00';
    }
    return formatMoney(this.montoIngresar);
  }

  get formattedCredited(): string {
    return formatMoney(this.creditedAmount);
  }

  selectQuickAmount(amount: number): void {
    if (this.isIngresandoDinero || this.phase !== 'form') {
      return;
    }
    this.montoIngresar = amount;
  }

  formatQuick(amount: number): string {
    return amount.toLocaleString('es-AR');
  }

  isQuickSelected(amount: number): boolean {
    return this.montoIngresar === amount;
  }

  async submit(): Promise<void> {
    if (!this.montoIngresar || this.montoIngresar <= 0) {
      this.toast.show('Ingresá un monto válido', 'error');
      return;
    }

    this.isIngresandoDinero = true;
    this.phase = 'crediting';
    const amount = this.montoIngresar;
    this.creditedAmount = amount;
    const startedAt = Date.now();

    try {
      await this.transferApi.ingresarDinero(amount);
      // Forzamos refresh de perfil + cuentas (el saldo ARS del dashboard usa arsAccount).
      await Promise.all([
        firstValueFrom(this.userDataStore.load(true)),
        firstValueFrom(this.accountService.getUserAccounts()),
      ]);

      const elapsed = Date.now() - startedAt;
      if (elapsed < this.minCreditingMs) {
        await this.wait(this.minCreditingMs - elapsed);
      }

      // Segunda pasada por si el backend aún no propagó el saldo.
      await Promise.all([
        firstValueFrom(this.userDataStore.load(true)),
        firstValueFrom(this.accountService.getUserAccounts()),
      ]);

      this.phase = 'success';
      await this.wait(this.successHoldMs);

      this.toast.show(`Ingreso exitoso de $${formatMoney(amount)}`, 'success');
      this.modalService.closeModal();
      this.success.emit(amount);
      this.closed.emit();
    } catch (error) {
      logger.error('Error ingresando dinero:', error);
      this.toast.show('Error al ingresar dinero', 'error');
      this.phase = 'form';
    } finally {
      this.isIngresandoDinero = false;
    }
  }

  close(): void {
    if (this.isIngresandoDinero || this.phase !== 'form') {
      return;
    }
    this.modalService.closeModal();
    this.closed.emit();
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('deposit-modal')) {
      this.close();
    }
  }

  private wait(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }
}
