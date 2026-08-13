import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  QueryList,
  ViewChildren,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { AdminService } from '../../../../services/admin/admin.service';
import { ToastService } from '../../../../services/toast/toast.service';
import { AdminMetrics, NamedCount, NamedMoney, TimePoint } from '../../../../models/admin-metrics';
import { formatMoney as formatMoneyShared } from '../../../../shared/utils/money-format';
import { logger } from '../../../../shared/utils/logger';

Chart.register(...registerables);

type ChartSlot =
  | 'registrations'
  | 'transactions'
  | 'volume'
  | 'txTypes'
  | 'usersStatus'
  | 'usersRole'
  | 'accounts'
  | 'loans'
  | 'cards'
  | 'volumeTypes';

@Component({
  selector: 'app-metrics-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './metrics-panel.html',
  styleUrls: ['./metrics-panel.css'],
})
export class MetricsPanelComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChildren('chartCanvas') private chartCanvases!: QueryList<ElementRef<HTMLCanvasElement>>;

  loading = true;
  error: string | null = null;
  metrics: AdminMetrics | null = null;

  private charts = new Map<string, Chart>();
  private viewReady = false;
  private pendingRender = false;

  private readonly palette = [
    '#0a66ff',
    '#0d9f6e',
    '#7c3aed',
    '#d97706',
    '#e11d48',
    '#0891b2',
    '#4263eb',
    '#64748b',
  ];

  constructor(
    private adminService: AdminService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.loadMetrics();
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.chartCanvases.changes.subscribe(() => {
      if (this.metrics) {
        this.renderCharts();
      }
    });
    if (this.pendingRender && this.metrics) {
      this.renderCharts();
    }
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  refresh(): void {
    this.loadMetrics();
  }

  formatMoney(amount: number): string {
    return formatMoneyShared(amount);
  }

  formatDateLabel(isoDate: string): string {
    const parts = isoDate.split('-');
    if (parts.length !== 3) return isoDate;
    return `${parts[2]}/${parts[1]}`;
  }

  private loadMetrics(): void {
    this.loading = true;
    this.error = null;
    this.destroyCharts();

    this.adminService.getMetrics().subscribe({
      next: (metrics) => {
        this.metrics = metrics;
        this.loading = false;
        if (this.viewReady) {
          // Wait a tick so *ngIf canvases exist.
          setTimeout(() => this.renderCharts(), 0);
        } else {
          this.pendingRender = true;
        }
      },
      error: (err) => {
        this.loading = false;
        this.error = 'No se pudieron cargar las métricas.';
        logger.error('Error cargando métricas admin:', err);
        this.toast.show('Error al cargar métricas', 'error');
      },
    });
  }

  private renderCharts(): void {
    if (!this.metrics) return;
    this.pendingRender = false;
    this.destroyCharts();

    const m = this.metrics;
    this.createLineChart(
      'registrations',
      m.registrationsLast14Days,
      'Altas',
      '#0a66ff',
      (p) => p.count
    );
    this.createLineChart(
      'transactions',
      m.transactionsLast14Days,
      'Movimientos',
      '#0d9f6e',
      (p) => p.count
    );
    this.createLineChart(
      'volume',
      m.volumeLast14Days,
      'Volumen',
      '#7c3aed',
      (p) => p.amount,
      true
    );
    this.createBarChart('txTypes', m.transactionsByType, 'Cantidad');
    this.createBarChartMoney('volumeTypes', m.volumeByType, 'Monto');
    this.createDoughnut('usersStatus', m.usersByStatus);
    this.createDoughnut('usersRole', m.usersByRole);
    this.createDoughnut('accounts', m.accountsByCurrency);
    this.createDoughnut('loans', m.loansByStatus);
    this.createDoughnut('cards', m.cardsByStatus);
  }

  private createLineChart(
    slot: ChartSlot,
    points: TimePoint[],
    label: string,
    color: string,
    valueFn: (p: TimePoint) => number,
    money = false
  ): void {
    const canvas = this.canvasFor(slot);
    if (!canvas) return;

    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: {
        labels: points.map((p) => this.formatDateLabel(p.date)),
        datasets: [
          {
            label,
            data: points.map(valueFn),
            borderColor: color,
            backgroundColor: color + '22',
            fill: true,
            tension: 0.35,
            pointRadius: 3,
            pointHoverRadius: 5,
            borderWidth: 2,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => {
                const v = Number(ctx.parsed.y ?? 0);
                return money ? `$${this.formatMoney(v)}` : `${v}`;
              },
            },
          },
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 7 },
          },
          y: {
            beginAtZero: true,
            ticks: {
              callback: (value) =>
                money ? `$${this.formatMoney(Number(value))}` : String(value),
            },
          },
        },
      },
    };

    this.charts.set(slot, new Chart(canvas, config));
  }

  private createBarChart(slot: ChartSlot, rows: NamedCount[], label: string): void {
    const canvas = this.canvasFor(slot);
    if (!canvas) return;

    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: rows.map((r) => r.name),
        datasets: [
          {
            label,
            data: rows.map((r) => r.value),
            backgroundColor: rows.map((_, i) => this.palette[i % this.palette.length]),
            borderRadius: 8,
            maxBarThickness: 42,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { display: false } },
          y: { beginAtZero: true, ticks: { precision: 0 } },
        },
      },
    };

    this.charts.set(slot, new Chart(canvas, config));
  }

  private createBarChartMoney(slot: ChartSlot, rows: NamedMoney[], label: string): void {
    const canvas = this.canvasFor(slot);
    if (!canvas) return;

    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: rows.map((r) => r.name),
        datasets: [
          {
            label,
            data: rows.map((r) => r.amount),
            backgroundColor: rows.map((_, i) => this.palette[i % this.palette.length]),
            borderRadius: 8,
            maxBarThickness: 42,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => `$${this.formatMoney(Number(ctx.parsed.y ?? 0))}`,
            },
          },
        },
        scales: {
          x: { grid: { display: false } },
          y: {
            beginAtZero: true,
            ticks: {
              callback: (value) => `$${this.formatMoney(Number(value))}`,
            },
          },
        },
      },
    };

    this.charts.set(slot, new Chart(canvas, config));
  }

  private createDoughnut(slot: ChartSlot, rows: NamedCount[]): void {
    const canvas = this.canvasFor(slot);
    if (!canvas) return;

    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels: rows.map((r) => r.name),
        datasets: [
          {
            data: rows.map((r) => r.value),
            backgroundColor: rows.map((_, i) => this.palette[i % this.palette.length]),
            borderWidth: 0,
            hoverOffset: 4,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '62%',
        plugins: {
          legend: {
            position: 'bottom',
            labels: { boxWidth: 10, usePointStyle: true, pointStyle: 'circle' },
          },
        },
      },
    };

    this.charts.set(slot, new Chart(canvas, config));
  }

  private canvasFor(slot: ChartSlot): HTMLCanvasElement | null {
    const el = this.chartCanvases?.find(
      (ref) => ref.nativeElement.dataset['chart'] === slot
    );
    return el?.nativeElement ?? null;
  }

  private destroyCharts(): void {
    for (const chart of this.charts.values()) {
      chart.destroy();
    }
    this.charts.clear();
  }
}
