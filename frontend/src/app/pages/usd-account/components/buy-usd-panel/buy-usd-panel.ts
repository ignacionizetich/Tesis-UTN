import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AccountService } from '../../../../services/account/account.service';
import { CotizationApi, CotizacionDolar } from '../../../../services/cotization-api/cotization.api';
import { ToastService } from '../../../../services/toast/toast.service';
import { formatMoney } from '../../../../shared/utils/money-format';
import { errorMessage } from '../../../../shared/utils/error-message';
import { logger } from '../../../../shared/utils/logger';
import { firstValueFrom } from 'rxjs';

export interface UsdTradeSuccess {
  newBalanceArs: number;
  newBalanceUsd: number;
  exchangeRate?: number;
}

@Component({
  selector: 'app-buy-usd-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './buy-usd-panel.html',
  styleUrls: ['./buy-usd-panel.css'],
})
export class BuyUsdPanelComponent implements OnInit {
  @Input() arsAccountId = '';
  @Input() usdAccountId = '';
  @Input() arsBalance = 0;
  @Input() usdBalance = 0;
  @Input() taxRate = 0.03;
  @Input() taxPercentage = 3;
  /** Fallback mientras carga la cotización real. */
  @Input() exchangeRate = 1100;

  @Output() closed = new EventEmitter<void>();
  @Output() success = new EventEmitter<UsdTradeSuccess>();
  @Output() quoteLoaded = new EventEmitter<number>();

  amountToBuyUsd: number | null = null;
  isBuyingUsd = false;
  isLoadingQuote = true;
  quoteError = false;
  quote: CotizacionDolar | null = null;

  estimatedUsdAmount = 0;
  estimatedTaxAmount = 0;
  estimatedTotalDebitado = 0;

  formatMoney = formatMoney;

  constructor(
    private accountService: AccountService,
    private cotizationApi: CotizationApi,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    void this.loadQuote();
  }

  get activeRate(): number {
    return this.quote?.venta && this.quote.venta > 0
      ? this.quote.venta
      : this.exchangeRate;
  }

  get canSubmit(): boolean {
    return (
      !!this.amountToBuyUsd &&
      this.amountToBuyUsd > 0 &&
      !this.isBuyingUsd &&
      !this.isLoadingQuote &&
      this.activeRate > 0
    );
  }

  async loadQuote(): Promise<void> {
    this.isLoadingQuote = true;
    this.quoteError = false;
    try {
      this.quote = await this.cotizationApi.getDolarOficial();
      this.quoteLoaded.emit(this.quote.venta);
      this.recalculate();
    } catch (error) {
      logger.error('No se pudo cargar la cotización:', error);
      this.quoteError = true;
      this.toast.show('No se pudo cargar la cotización. Usamos una estimación local.', 'error');
    } finally {
      this.isLoadingQuote = false;
    }
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

  onAmountChange(value: number | null): void {
    this.amountToBuyUsd = value;
    this.recalculate();
  }

  private recalculate(): void {
    const value = this.amountToBuyUsd;
    if (value && value > 0) {
      this.estimatedTaxAmount = value * this.taxRate;
      this.estimatedTotalDebitado = value + this.estimatedTaxAmount;
      this.estimatedUsdAmount =
        this.activeRate > 0 ? value / this.activeRate : 0;
    } else {
      this.estimatedUsdAmount = 0;
      this.estimatedTaxAmount = 0;
      this.estimatedTotalDebitado = 0;
    }
  }

  close(): void {
    if (this.isBuyingUsd) {
      return;
    }
    this.closed.emit();
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('buy-usd-modal')) {
      this.close();
    }
  }

  async submit(): Promise<void> {
    if (!this.amountToBuyUsd || this.amountToBuyUsd <= 0) {
      this.toast.show('Ingresá un monto válido', 'error');
      return;
    }

    const totalDebitado = this.amountToBuyUsd * (1 + this.taxRate);
    if (totalDebitado > this.arsBalance) {
      this.toast.show(
        `Saldo insuficiente. Necesitás $${totalDebitado.toFixed(2)} ARS (incluye comisión del ${this.taxPercentage}%)`,
        'error'
      );
      return;
    }

    this.isBuyingUsd = true;

    try {
      const response = await firstValueFrom(
        this.accountService.buyUsd(
          this.arsAccountId,
          this.usdAccountId,
          this.amountToBuyUsd
        )
      );

      if (response?.success) {
        this.toast.show(`Compra exitosa: $${response.amountUsd.toFixed(2)} USD`, 'success');
        this.success.emit({
          newBalanceArs: response.newBalanceArs,
          newBalanceUsd: response.newBalanceUsd,
          exchangeRate: response.exchangeRate,
        });
        this.closed.emit();
      } else {
        this.toast.show(response?.message || 'Error en la compra', 'error');
      }
    } catch (error: unknown) {
      logger.error('Error comprando USD:', error);
      this.toast.show(errorMessage(error, 'Error al comprar dólares'), 'error');
    } finally {
      this.isBuyingUsd = false;
    }
  }
}
