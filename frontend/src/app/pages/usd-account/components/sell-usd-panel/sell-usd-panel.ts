import {
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AccountService } from '../../../../services/account/account.service';
import { ToastService } from '../../../../services/toast/toast.service';
import { formatMoney } from '../../../../shared/utils/money-format';
import { errorMessage } from '../../../../shared/utils/error-message';
import { UsdTradeSuccess } from '../buy-usd-panel/buy-usd-panel';

@Component({
  selector: 'app-sell-usd-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sell-usd-panel.html',
  styleUrls: ['../../styles/usd-modals.css'],
})
export class SellUsdPanelComponent {
  @Input() arsAccountId = '';
  @Input() usdAccountId = '';
  @Input() arsBalance = 0;
  @Input() usdBalance = 0;
  @Input() taxRate = 0.03;
  @Input() taxPercentage = 3;
  @Input() exchangeRate = 1100;

  @Output() closed = new EventEmitter<void>();
  @Output() success = new EventEmitter<UsdTradeSuccess>();

  amountToSellUsd: number | null = null;
  isSellingUsd = false;
  estimatedArsAmount = 0;
  estimatedTaxAmount = 0;
  estimatedTotalDebitado = 0;

  formatMoney = formatMoney;

  constructor(
    private accountService: AccountService,
    private toast: ToastService
  ) {}

  onAmountChange(value: number | null): void {
    this.amountToSellUsd = value;
    if (value && value > 0) {
      this.estimatedTaxAmount = value * this.taxRate;
      this.estimatedTotalDebitado = value + this.estimatedTaxAmount;
      this.estimatedArsAmount = value * this.exchangeRate;
    } else {
      this.estimatedArsAmount = 0;
      this.estimatedTaxAmount = 0;
      this.estimatedTotalDebitado = 0;
    }
  }

  async submit(): Promise<void> {
    if (!this.amountToSellUsd || this.amountToSellUsd <= 0) {
      this.toast.show('Por favor ingrese un monto válido', 'error');
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
      const response = await this.accountService
        .sellUsd(this.usdAccountId, this.arsAccountId, this.amountToSellUsd)
        .toPromise();

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
      console.error('Error vendiendo USD:', error);
      this.toast.show(errorMessage(error, 'Error al vender dólares'), 'error');
    } finally {
      this.isSellingUsd = false;
    }
  }
}
