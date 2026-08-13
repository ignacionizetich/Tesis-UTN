import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../../services/admin/admin.service';
import { ToastService } from '../../../../services/toast/toast.service';
import { LoanRateItem } from '../../../../models/loan-rates';
import { formatMoney } from '../../../../shared/utils/money-format';
import { logger } from '../../../../shared/utils/logger';

interface EditableRate {
  installments: number;
  monthlyRatePercent: number;
  originalPercent: number;
}

@Component({
  selector: 'app-loan-rates-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './loan-rates-panel.html',
  styleUrls: ['./loan-rates-panel.css'],
})
export class LoanRatesPanelComponent implements OnInit {
  loading = true;
  saving = false;
  error: string | null = null;
  updatedAt: string | null = null;
  rates: EditableRate[] = [];
  previewPrincipal = 10000;

  readonly minPercent = 0.5;
  readonly maxPercent = 15;
  readonly step = 0.1;

  formatMoney = formatMoney;

  constructor(
    private adminService: AdminService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  get dirty(): boolean {
    return this.rates.some((r) => r.monthlyRatePercent !== r.originalPercent);
  }

  get ascendingHint(): boolean {
    if (this.rates.length < 3) return true;
    const sorted = [...this.rates].sort((a, b) => a.installments - b.installments);
    return (
      sorted[0].monthlyRatePercent <= sorted[1].monthlyRatePercent &&
      sorted[1].monthlyRatePercent <= sorted[2].monthlyRatePercent
    );
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.adminService.getLoanRates().subscribe({
      next: (res) => {
        this.rates = res.rates.map((r) => this.toEditable(r));
        this.updatedAt = res.updatedAt;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = 'No se pudieron cargar las tasas.';
        logger.error('Error cargando tasas de préstamos', err);
        this.toast.show('Error al cargar tasas', 'error');
      },
    });
  }

  bump(rate: EditableRate, delta: number): void {
    const next = Math.round((rate.monthlyRatePercent + delta) * 10) / 10;
    rate.monthlyRatePercent = this.clamp(next);
  }

  onPercentInput(rate: EditableRate, value: string | number): void {
    const parsed = typeof value === 'number' ? value : Number(value);
    if (Number.isNaN(parsed)) return;
    rate.monthlyRatePercent = this.clamp(Math.round(parsed * 10) / 10);
  }

  resetRate(rate: EditableRate): void {
    rate.monthlyRatePercent = rate.originalPercent;
  }

  resetAll(): void {
    this.rates.forEach((r) => (r.monthlyRatePercent = r.originalPercent));
  }

  save(): void {
    if (!this.dirty || this.saving) return;
    this.saving = true;
    this.adminService
      .updateLoanRates({
        rates: this.rates.map((r) => ({
          installments: r.installments,
          monthlyRatePercent: r.monthlyRatePercent,
        })),
      })
      .subscribe({
        next: (res) => {
          this.rates = res.rates.map((r) => this.toEditable(r));
          this.updatedAt = res.updatedAt;
          this.saving = false;
          this.toast.show('Tasas de préstamos actualizadas', 'success');
        },
        error: (err) => {
          this.saving = false;
          const msg =
            err?.error?.mensaje ||
            err?.error?.message ||
            'No se pudieron guardar las tasas';
          logger.error('Error guardando tasas', err);
          this.toast.show(msg, 'error');
        },
      });
  }

  previewInstallment(rate: EditableRate): number {
    const n = rate.installments;
    const i = rate.monthlyRatePercent / 100;
    const p = this.previewPrincipal;
    if (i <= 0) return Math.round((p / n) * 100) / 100;
    const factor = Math.pow(1 + i, n);
    const cuota = (p * i * factor) / (factor - 1);
    return Math.round(cuota * 100) / 100;
  }

  previewTotal(rate: EditableRate): number {
    return Math.round(this.previewInstallment(rate) * rate.installments * 100) / 100;
  }

  private toEditable(r: LoanRateItem): EditableRate {
    return {
      installments: r.installments,
      monthlyRatePercent: r.monthlyRatePercent,
      originalPercent: r.monthlyRatePercent,
    };
  }

  private clamp(value: number): number {
    return Math.min(this.maxPercent, Math.max(this.minPercent, value));
  }
}
