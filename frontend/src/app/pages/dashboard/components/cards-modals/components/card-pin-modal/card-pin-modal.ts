import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VirtualCardApi } from '../../../../../../services/virtual-card/virtual-card.api';
import { ToastService } from '../../../../../../services/toast/toast.service';
import { HttpErrorResponse } from '@angular/common/http';

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

  get canSubmit(): boolean {
    if (this.submitting) return false;
    if (!/^\d{6}$/.test(this.pin)) return false;
    if (!this.pinConfigured && this.pin !== this.confirmPin) return false;
    return true;
  }

  async submit(): Promise<void> {
    if (!this.canSubmit) {
      this.toast.show('Ingresá un PIN de 6 dígitos válido', 'error');
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
      const msg =
        error instanceof HttpErrorResponse
          ? error.error?.message || error.error?.error
          : null;
      this.toast.show(msg || this.virtualCardApi.handleError(error, 'Error con el PIN'), 'error');
    } finally {
      this.submitting = false;
    }
  }
}
