import {
  Component,
  EventEmitter,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TransferApi } from '../../../../services/transfer-api/transfer.api';
import { ToastService } from '../../../../services/toast/toast.service';
import { ModalService } from '../../../../services/modal/modal.service';
import { logger } from '../../../../shared/utils/logger';

@Component({
  selector: 'app-deposit-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './deposit-modal.html',
  styleUrls: ['../../styles/modals-shared.css'],
})
export class DepositModalComponent {
  montoIngresar: number | null = null;
  isIngresandoDinero = false;

  @Output() closed = new EventEmitter<void>();
  /** Emitido tras depósito exitoso (para animar balance en el parent). */
  @Output() success = new EventEmitter<number>();
  @Output() backdropClick = new EventEmitter<MouseEvent>();

  constructor(
    private transferApi: TransferApi,
    private toast: ToastService,
    private modalService: ModalService
  ) {}

  async submit(): Promise<void> {
    if (!this.montoIngresar || this.montoIngresar <= 0) {
      this.toast.show('Por favor ingrese un monto válido', 'error');
      return;
    }

    this.isIngresandoDinero = true;
    const amount = this.montoIngresar;

    try {
      await this.transferApi.ingresarDinero(amount);
      this.toast.show(`Ingreso exitoso de $${amount}`, 'success');
      this.modalService.closeModal();
      this.success.emit(amount);
      this.closed.emit();
    } catch (error) {
      logger.error('Error ingresando dinero:', error);
      this.toast.show('Error al ingresar dinero', 'error');
    } finally {
      this.isIngresandoDinero = false;
    }
  }

  close(): void {
    this.modalService.closeModal();
    this.closed.emit();
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal')) {
      this.close();
    }
  }
}
