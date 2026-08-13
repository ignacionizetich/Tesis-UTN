import {
  Component,
  EventEmitter,
  Input,
  Output,
  ViewEncapsulation,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { formatMoney } from '../../../../shared/utils/money-format';

@Component({
  selector: 'app-sell-usd-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sell-usd-panel.html',
  styleUrls: ['../../usd-account.css'],
  encapsulation: ViewEncapsulation.None,
})
export class SellUsdPanelComponent {
  @Input() arsBalance = 0;
  @Input() usdBalance = 0;
  @Input() taxPercentage = 3;
  @Input() estimatedArsAmount = 0;
  @Input() estimatedTaxAmount = 0;
  @Input() estimatedTotalDebitado = 0;
  @Input() isSellingUsd = false;

  @Input() amountToSellUsd: number | null = null;
  @Output() amountToSellUsdChange = new EventEmitter<number | null>();

  @Output() closed = new EventEmitter<void>();
  @Output() submitted = new EventEmitter<void>();
  @Output() amountChanged = new EventEmitter<void>();

  formatMoney = formatMoney;

  onAmountChange(value: number | null): void {
    this.amountToSellUsdChange.emit(value);
    this.amountChanged.emit();
  }
}
