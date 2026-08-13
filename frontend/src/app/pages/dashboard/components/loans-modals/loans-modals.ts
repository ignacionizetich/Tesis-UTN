import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { ModalService } from '../../../../services/modal/modal.service';
import { LoanApi } from '../../../../services/loan/loan.api';
import { ToastService } from '../../../../services/toast/toast.service';
import { UserDataStore } from '../../../../services/user-data-store/user-data.store';
import { TransactionService } from '../../../../services/transaction/transaction.service';
import { LoanDetail, LoanSimulation, LoanSummary } from '../../../../models/loan';
import { formatMoney } from '../../../../shared/utils/money-format';
import { logger } from '../../../../shared/utils/logger';

type LoanView = 'list' | 'simulate' | 'detail';

@Component({
  selector: 'app-loans-modals',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './loans-modals.html',
  styleUrls: ['./loans-modals.css'],
})
export class LoansModalsComponent implements OnInit, OnDestroy {
  open = false;
  view: LoanView = 'list';
  loans: LoanSummary[] = [];
  selected: LoanDetail | null = null;
  simulation: LoanSimulation | null = null;
  loading = false;
  busy = false;

  principal = 10000;
  installments = 6;
  readonly termOptions = [3, 6, 12];
  rateByTerm: Record<number, number> = { 3: 3, 6: 4, 12: 5.5 };

  formatMoney = formatMoney;

  private subscriptions: Subscription[] = [];

  constructor(
    private modalService: ModalService,
    private loanApi: LoanApi,
    private toast: ToastService,
    private userDataStore: UserDataStore,
    private transactionService: TransactionService
  ) {}

  ngOnInit(): void {
    this.subscriptions.push(
      this.modalService.modalState$.subscribe((state) => {
        this.open = state.currentModal === 'loans';
        if (this.open) {
          this.view = 'list';
          this.selected = null;
          this.simulation = null;
          void this.loadRates();
          void this.loadLoans();
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((s) => s.unsubscribe());
  }

  get activeLoan(): LoanSummary | undefined {
    return this.loans.find((l) => l.status === 'ACTIVE');
  }

  get selectedPaidCount(): number {
    if (!this.selected) return 0;
    if (this.selected.paidCount != null) return this.selected.paidCount;
    return this.selected.installments.filter((i) => i.status === 'PAID').length;
  }

  close(): void {
    this.modalService.closeModal();
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('loans-modal')) {
      this.close();
    }
  }

  goSimulate(): void {
    this.view = 'simulate';
    this.simulation = null;
    void this.runSimulate();
  }

  goList(): void {
    this.view = 'list';
    this.selected = null;
    void this.loadLoans();
  }

  async runSimulate(): Promise<void> {
    this.busy = true;
    try {
      this.simulation = await this.loanApi.simulate(this.principal, this.installments);
    } catch (error) {
      this.toast.show(this.loanApi.handleError(error, 'No se pudo simular'), 'error');
      this.simulation = null;
    } finally {
      this.busy = false;
    }
  }

  async acceptLoan(): Promise<void> {
    if (!this.simulation || this.busy) return;
    this.busy = true;
    try {
      const detail = await this.loanApi.accept(this.principal, this.installments);
      this.toast.show('Préstamo acreditado en tu cuenta ARS', 'success');
      this.userDataStore.load(true).subscribe();
      void this.transactionService.loadAllTransactions(true).catch(() => undefined);
      this.selected = detail;
      this.view = 'detail';
      await this.loadLoans();
    } catch (error) {
      this.toast.show(this.loanApi.handleError(error, 'No se pudo aceptar el préstamo'), 'error');
    } finally {
      this.busy = false;
    }
  }

  async openDetail(loan: LoanSummary): Promise<void> {
    this.busy = true;
    try {
      this.selected = await this.loanApi.detail(loan.id);
      this.view = 'detail';
    } catch (error) {
      this.toast.show(this.loanApi.handleError(error, 'No se pudo cargar el préstamo'), 'error');
    } finally {
      this.busy = false;
    }
  }

  async payNext(): Promise<void> {
    if (!this.selected || this.busy) return;
    this.busy = true;
    try {
      this.selected = await this.loanApi.payNext(this.selected.id);
      this.toast.show('Cuota pagada', 'success');
      this.userDataStore.load(true).subscribe();
      void this.transactionService.loadAllTransactions(true).catch(() => undefined);
      await this.loadLoans();
    } catch (error) {
      this.toast.show(this.loanApi.handleError(error, 'No se pudo pagar la cuota'), 'error');
    } finally {
      this.busy = false;
    }
  }

  statusLabel(status: string): string {
    return status === 'PAID_OFF' ? 'Cancelado' : 'Activo';
  }

  rateLabel(term: number): string {
    const percent = this.rateByTerm[term];
    if (percent == null) return '—';
    const formatted = Number.isInteger(percent)
      ? String(percent)
      : String(percent).replace('.', ',');
    return `${formatted}% mens.`;
  }

  private async loadRates(): Promise<void> {
    try {
      const res = await this.loanApi.rates();
      const next: Record<number, number> = { ...this.rateByTerm };
      for (const row of res.rates) {
        next[row.installments] = row.monthlyRatePercent;
      }
      this.rateByTerm = next;
    } catch (error) {
      logger.error('Error cargando tasas de préstamos', error);
    }
  }

  private async loadLoans(): Promise<void> {
    this.loading = true;
    try {
      this.loans = await this.loanApi.list();
    } catch (error) {
      logger.error('Error listando préstamos', error);
      this.toast.show(this.loanApi.handleError(error, 'No se pudieron cargar los préstamos'), 'error');
    } finally {
      this.loading = false;
    }
  }
}
