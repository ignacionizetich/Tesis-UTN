import {
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AccountService } from '../../../../services/account-service/account.service';
import { ToastService } from '../../../../services/toast-service/toast.service';
import { formatMoney } from '../../../../shared/utils/money-format';
import { errorMessage } from '../../../../shared/utils/error-message';

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
  styleUrls: ['../../styles/usd-modals.css'],
})
export class BuyUsdPanelComponent {
  @Input() arsAccountId = '';
  @Input() usdAccountId = '';
  @Input() arsBalance = 0;
  @Input() usdBalance = 0;
  @Input() taxRate = 0.03;
  @Input() taxPercentage = 3;
  @Input() exchangeRate = 1100;

  @Output() closed = new EventEmitter<void>();
  @Output() success = new EventEmitter<UsdTradeSuccess>();

  amountToBuyUsd: number | null = null;
  isBuyingUsd = false;
  estimatedUsdAmount = 0;
  estimatedTaxAmount = 0;
  estimatedTotalDebitado = 0;

  formatMoney = formatMoney;

  constructor(
    private accountService: AccountService,
    private toast: ToastService
  ) {}

  onAmountChange(value: number | null): void {
    this.amountToBuyUsd = value;
    if (value && value > 0) {
      this.estimatedTaxAmount = value * this.taxRate;
      this.estimatedTotalDebitado = value + this.estimatedTaxAmount;
      this.estimatedUsdAmount =
        this.exchangeRate > 0 ? value / this.exchangeRate : 0;
    } else {
      this.estimatedUsdAmount = 0;
      this.estimatedTaxAmount = 0;
      this.estimatedTotalDebitado = 0;
    }
  }

  async submit(): Promise<void> {
    if (!this.amountToBuyUsd || this.amountToBuyUsd <= 0) {
      this.toast.show('Por favor ingrese un monto válido', 'error');
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
      const response = await this.accountService
        .buyUsd(this.arsAccountId, this.usdAccountId, this.amountToBuyUsd)
        .toPromise();

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
      console.error('Error comprando USD:', error);
      this.toast.show(errorMessage(error, 'Error al comprar dólares'), 'error');
    } finally {
      this.isBuyingUsd = false;
    }
  }
}
