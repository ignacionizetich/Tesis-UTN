import {
  Component,
  EventEmitter,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TaxApi, TaxCalculationResult } from '../../../../services/tax-api/tax.api';
import { ToastService } from '../../../../services/toast/toast.service';
import { ModalService } from '../../../../services/modal/modal.service';
import { formatMoney } from '../../../../shared/utils/money-format';
import { logger } from '../../../../shared/utils/logger';

export interface TaxResultView {
  currency: 'ARS' | 'USD';
  inputAmount: number;
  montoOriginal: number;
  iva: number;
  alicuotaIva: number;
  totalFinal: number;
  montoUsd?: number;
  precioDolar?: number;
  dolarCompra?: number;
  dolarVenta?: number;
  nombreCotizacion?: string | null;
  casa?: string | null;
  fechaActualizacion?: string | null;
}

@Component({
  selector: 'app-tax-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tax-modal.html',
  styleUrls: ['./tax-modal.css'],
})
export class TaxModalComponent {
  selectedCurrency: 'ARS' | 'USD' = 'ARS';
  taxMonto: number | null = null;
  result: TaxResultView | null = null;
  isCalculating = false;

  @Output() closed = new EventEmitter<void>();
  @Output() backdropClick = new EventEmitter<MouseEvent>();

  constructor(
    private taxApi: TaxApi,
    private toast: ToastService,
    private modalService: ModalService
  ) {}

  get canCalculate(): boolean {
    return !!this.taxMonto && this.taxMonto > 0 && !this.isCalculating;
  }

  formatAmount(value: number): string {
    return formatMoney(value);
  }

  formatQuoteDate(iso: string | null | undefined): string {
    if (!iso) {
      return '';
    }
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) {
      return iso;
    }
    return new Intl.DateTimeFormat('es-AR', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(date);
  }

  selectCurrency(currency: 'ARS' | 'USD'): void {
    if (this.selectedCurrency === currency) {
      return;
    }
    this.selectedCurrency = currency;
    this.result = null;
  }

  async calculate(): Promise<void> {
    if (!this.taxMonto || this.taxMonto <= 0) {
      this.toast.show('Ingresá un monto válido', 'error');
      return;
    }

    this.isCalculating = true;
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
        alicuotaIva: data.alicuotaIva ?? 21,
        totalFinal: data.totalFinal,
        montoUsd: data.montoUsd,
        precioDolar: data.precioDolar,
        dolarCompra: data.dolarCompra,
        dolarVenta: data.dolarVenta ?? data.precioDolar,
        nombreCotizacion: data.nombreCotizacion,
        casa: data.casa,
        fechaActualizacion: data.fechaActualizacion,
      };
    } catch (error) {
      logger.error('Error calculando impuestos:', error);
      this.toast.show('Error al calcular impuestos', 'error');
      this.result = null;
    } finally {
      this.isCalculating = false;
    }
  }

  close(): void {
    if (this.isCalculating) {
      return;
    }
    this.modalService.closeModal();
    this.closed.emit();
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('tax-modal')) {
      this.close();
    }
  }
}
