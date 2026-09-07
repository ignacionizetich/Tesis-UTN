import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VirtualCardApi } from '../../../../../../services/virtual-card/virtual-card.api';
import { ToastService } from '../../../../../../services/toast/toast.service';
import { cardPinIssues } from '../../../../../../shared/validators/auth.validators';
import { httpStatus } from '../../../../../../shared/utils/error-message';

@Component({
  selector: 'app-card-pin-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './card-pin-modal.html',
  styleUrls: ['./card-pin-modal.css'],
})
export class CardPinModalComponent {
  @Input() pinConfigured = false;
  @Input() cardLabel = 'ARS';

  @Output() closed = new EventEmitter<void>();
  @Output() unlocked = new EventEmitter<void>();
  @Output() pinCreated = new EventEmitter<void>();

  pin = '';
  confirmPin = '';
  submitting = false;

  constructor(
    private virtualCardApi: VirtualCardApi,
    private toast: ToastService
  ) {}

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('card-pin-modal')) {
      this.closed.emit();
    }
  }

  get pinError(): string | null {
    if (!this.pin || this.pinConfigured) {
      return null;
    }
    const issues = cardPinIssues(this.pin);
    if (!issues) {
      return null;
    }
    if (issues['pinFormat']) {
      return 'El PIN debe tener exactamente 6 dígitos';
    }
    if (issues['pinRepeated']) {
      return 'El PIN no puede tener todos los dígitos iguales';
    }
    if (issues['pinSequential']) {
      return 'El PIN no puede ser una secuencia de dígitos consecutivos';
    }
    if (issues['pinCommon']) {
      return 'El PIN elegido es demasiado común, probá con otro';
    }
    return 'Elegí un PIN más seguro';
  }

  get canSubmit(): boolean {
    if (this.submitting) return false;
    if (this.pinConfigured) {
      return /^\d{6}$/.test(this.pin);
    }
    return !cardPinIssues(this.pin) && this.pin === this.confirmPin;
  }

  async submit(): Promise<void> {
    if (!this.canSubmit) {
      this.toast.show(this.pinError || 'Ingresá un PIN de 6 dígitos válido', 'error');
      return;
    }
    this.submitting = true;
    try {
      const response = this.pinConfigured
        ? await this.virtualCardApi.verifyPin(this.pin)
        : await this.virtualCardApi.setPin(this.pin, this.confirmPin);

      if (!response.success) {
        this.toast.show(response.message || 'No se pudo validar el PIN', 'error');
        return;
      }
      if (!this.pinConfigured) {
        this.pinCreated.emit();
      }
      this.toast.show(response.message || 'Listo', 'success');
      this.unlocked.emit();
    } catch (error: unknown) {
      const locked = httpStatus(error) === 423;
      this.toast.show(
        this.virtualCardApi.handleError(
          error,
          locked ? 'PIN bloqueado. Probá de nuevo en unos minutos.' : 'Error con el PIN'
        ),
        'error'
      );
    } finally {
      this.submitting = false;
    }
  }
}
