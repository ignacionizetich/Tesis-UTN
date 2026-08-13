import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AccountService } from '../../../../services/account/account.service';
import { CotizationApi, CotizacionDolar } from '../../../../services/cotization-api/cotization.api';
import { ToastService } from '../../../../services/toast/toast.service';
import { formatMoney } from '../../../../shared/utils/money-format';
import { errorMessage } from '../../../../shared/utils/error-message';
import { UsdTradeSuccess } from '../buy-usd-panel/buy-usd-panel';
import { logger } from '../../../../shared/utils/logger';

@Component({
  selector: 'app-sell-usd-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sell-usd-panel.html',
  styleUrls: ['./sell-usd-panel.css'],
})
export class SellUsdPanelComponent implements OnInit {
  @Input() arsAccountId = '';
  @Input() usdAccountId = '';
  @Input() arsBalance = 0;
  @Input() usdBalance = 0;
  @Input() taxRate = 0.03;
  @Input() taxPercentage = 3;
  /** Fallback mientras carga la cotización real (compra). */
  @Input() exchangeRate = 1100;

  @Output() closed = new EventEmitter<void>();
  @Output() success = new EventEmitter<UsdTradeSuccess>();

  amountToSellUsd: number | null = null;
  isSellingUsd = false;
  isLoadingQuote = true;
  quoteError = false;
  quote: CotizacionDolar | null = null;

  estimatedArsAmount = 0;
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

  /** Venta USD→ARS usa cotización de compra. */
  get activeRate(): number {
    return this.quote?.compra && this.quote.compra > 0
      ? this.quote.compra
      : this.exchangeRate;
  }

  get canSubmit(): boolean {
    return (
      !!this.amountToSellUsd &&
      this.amountToSellUsd > 0 &&
      !this.isSellingUsd &&
      !this.isLoadingQuote &&
      this.activeRate > 0
    );
  }

  async loadQuote(): Promise<void> {
    this.isLoadingQuote = true;
    this.quoteError = false;
    try {
      this.quote = await this.cotizationApi.getDolarOficial();
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
    this.amountToSellUsd = value;
    this.recalculate();
  }

  private recalculate(): void {
    const value = this.amountToSellUsd;
    if (value && value > 0) {
      this.estimatedTaxAmount = value * this.taxRate;
      this.estimatedTotalDebitado = value + this.estimatedTaxAmount;
      this.estimatedArsAmount = value * this.activeRate;
    } else {
      this.estimatedArsAmount = 0;
      this.estimatedTaxAmount = 0;
      this.estimatedTotalDebitado = 0;
    }
  }

  close(): void {
    if (this.isSellingUsd) {
      return;
    }
    this.closed.emit();
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('sell-usd-modal')) {
      this.close();
    }
  }

  async submit(): Promise<void> {
    if (!this.amountToSellUsd || this.amountToSellUsd <= 0) {
      this.toast.show('Ingresá un monto válido', 'error');
      return;
    }

    const totalDebitado = this.amountToSellUsd * (1 + this.taxRate);
    if (totalDebitado > this.usdBalance) {
      this.toast.show(
        `Saldo insuficiente. Necesitás $${totalDebitado.toFixed(2)} USD (incluye comisión del ${this.taxPercentage}%)`,
        'error'
      );
      return;
    }

    this.isSellingUsd = true;

    try {
      const response = await firstValueFrom(
        this.accountService.sellUsd(
          this.usdAccountId,
          this.arsAccountId,
          this.amountToSellUsd
        )
      );

      if (response?.success) {
        this.toast.show(`Venta exitosa: $${response.amountArs.toFixed(2)} ARS`, 'success');
        this.success.emit({
          newBalanceArs: response.newBalanceArs,
          newBalanceUsd: response.newBalanceUsd,
          exchangeRate: response.exchangeRate,
        });
        this.closed.emit();
      } else {
        this.toast.show(response?.message || 'Error en la venta', 'error');
      }
    } catch (error: unknown) {
      logger.error('Error vendiendo USD:', error);
      this.toast.show(errorMessage(error, 'Error al vender dólares'), 'error');
    } finally {
      this.isSellingUsd = false;
    }
  }
}
