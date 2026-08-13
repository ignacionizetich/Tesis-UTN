import {
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { formatMoney } from '../../../../shared/utils/money-format';

@Component({
  selector: 'app-buy-usd-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './buy-usd-panel.html',
  styleUrls: ['../../styles/usd-modals.css'],
})
export class BuyUsdPanelComponent {
  @Input() arsBalance = 0;
  @Input() usdBalance = 0;
  @Input() taxPercentage = 3;
  @Input() estimatedUsdAmount = 0;
  @Input() estimatedTaxAmount = 0;
  @Input() estimatedTotalDebitado = 0;
  @Input() isBuyingUsd = false;

  @Input() amountToBuyUsd: number | null = null;
  @Output() amountToBuyUsdChange = new EventEmitter<number | null>();

  @Output() closed = new EventEmitter<void>();
  @Output() submitted = new EventEmitter<void>();
  @Output() amountChanged = new EventEmitter<void>();

  formatMoney = formatMoney;

  onAmountChange(value: number | null): void {
    this.amountToBuyUsdChange.emit(value);
    this.amountChanged.emit();
  }
}
