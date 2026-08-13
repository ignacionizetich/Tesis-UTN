import {
  Component,
  EventEmitter,
  Output,
  ViewEncapsulation,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TaxApi, TaxCalculationResult } from '../../../../services/tax-api/tax.api';
import { ToastService } from '../../../../services/toast-service/toast.service';
import { ModalService } from '../../../../services/modal-service/modal-service';
import { formatMoney } from '../../../../shared/utils/money-format';

export interface TaxResultView {
  currency: 'ARS' | 'USD';
  inputAmount: number;
  montoOriginal: number;
  iva: number;
  totalFinal: number;
  precioDolar?: number;
}

@Component({
  selector: 'app-tax-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tax-modal.html',
  styleUrls: ['../../styles/modals-shared.css'],
  encapsulation: ViewEncapsulation.None,
})
export class TaxModalComponent {
  showTaxForm = false;
  selectedCurrency: 'ARS' | 'USD' = 'ARS';
  taxMonto = 0;
  result: TaxResultView | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() backdropClick = new EventEmitter<MouseEvent>();

  constructor(
    private taxApi: TaxApi,
    private toast: ToastService,
    private modalService: ModalService
  ) {}

  formatAmount(value: number): string {
    return formatMoney(value);
  }

  selectCurrency(currency: 'ARS' | 'USD'): void {
    this.selectedCurrency = currency;
    this.showTaxForm = true;
    this.taxMonto = 0;
    this.result = null;
  }

  async calculate(): Promise<void> {
    if (!this.taxMonto || this.taxMonto <= 0) {
      this.toast.show('Por favor ingrese un monto válido', 'error');
      return;
    }

    try {
      const data: TaxCalculationResult =
        this.selectedCurrency === 'ARS'
          ? await this.taxApi.calculateTaxesARS(this.taxMonto)
          : await this.taxApi.calculateTaxesUSD(this.taxMonto);

      this.result = {
        currency: this.selectedCurrency,
        inputAmount: this.taxMonto,
        montoOriginal: data.montoOriginal,
        iva: data.iva,
        totalFinal: data.totalFinal,
        precioDolar: data.precioDolar,
      };
    } catch (error) {
      console.error('Error calculando impuestos:', error);
      this.toast.show('Error al calcular impuestos', 'error');
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
