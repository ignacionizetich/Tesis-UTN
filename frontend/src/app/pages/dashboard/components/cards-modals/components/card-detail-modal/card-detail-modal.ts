import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VirtualCardApi } from '../../../../../../services/virtual-card/virtual-card.api';
import { ToastService } from '../../../../../../services/toast/toast.service';
import {
  CardAuditEvent,
  VirtualCardReveal,
  VirtualCardSummary,
} from '../../../../../../models/virtual-card';
import { formatMoney } from '../../../../../../shared/utils/money-format';
import { logger } from '../../../../../../shared/utils/logger';

@Component({
  selector: 'app-card-detail-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './card-detail-modal.html',
  styleUrls: ['./card-detail-modal.css'],
})
export class CardDetailModalComponent implements OnInit, OnChanges, OnDestroy {
  @Input({ required: true }) card!: VirtualCardSummary;

  @Output() closed = new EventEmitter<void>();
  @Output() back = new EventEmitter<void>();
  @Output() cardUpdated = new EventEmitter<VirtualCardSummary>();

  @ViewChild('tiltShell') tiltShell?: ElementRef<HTMLElement>;

  flipped = false;
  loadingReveal = true;
  revealError = false;
  reveal: VirtualCardReveal | null = null;
  audit: CardAuditEvent[] = [];
  editingLimit = false;
  limitDraft = 0;
  busy = false;
  confirmingCancel = false;
  tiltActive = false;
  tiltEnabled = true;

  formatMoney = formatMoney;

  private rafId = 0;
  private targetRotateX = 0;
  private targetRotateY = 0;
  private currentRotateX = 0;
  private currentRotateY = 0;
  private animating = false;

  constructor(
    private virtualCardApi: VirtualCardApi,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    if (typeof window !== 'undefined') {
      this.tiltEnabled =
        !window.matchMedia('(prefers-reduced-motion: reduce)').matches &&
        !window.matchMedia('(hover: none)').matches;
    }
    void this.loadSensitive();
    void this.loadAudit();
  }

  ngOnDestroy(): void {
    if (this.rafId) {
      cancelAnimationFrame(this.rafId);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['card'] && !changes['card'].firstChange) {
      void this.loadSensitive();
    }
  }

  get isUsd(): boolean {
    return this.card.currency === 'USD';
  }

  get isCancelled(): boolean {
    return this.card.status === 'CANCELLED';
  }

  get expLabel(): string {
    if (!this.reveal) {
      if (this.card.expMonth && this.card.expYear) {
        const m = String(this.card.expMonth).padStart(2, '0');
        const y = String(this.card.expYear).slice(-2);
        return `${m}/${y}`;
      }
      return '••/••';
    }
    const m = String(this.reveal.expMonth).padStart(2, '0');
    const y = String(this.reveal.expYear).slice(-2);
    return `${m}/${y}`;
  }

  get statusLabel(): string {
    if (this.card.status === 'CANCELLED') return 'Dada de baja';
    if (this.card.status === 'PAUSED') return 'Pausada';
    if (this.card.expired) return 'Vencida';
    return 'Activa';
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('card-detail-modal')) {
      this.closed.emit();
    }
  }

  onTiltMove(event: PointerEvent): void {
    if (!this.tiltEnabled) return;
    const el = event.currentTarget as HTMLElement;
    const rect = el.getBoundingClientRect();
    const px = (event.clientX - rect.left) / rect.width;
    const py = (event.clientY - rect.top) / rect.height;
    const clampedX = Math.min(1, Math.max(0, px));
    const clampedY = Math.min(1, Math.max(0, py));

    const maxTilt = 14;
    this.targetRotateY = (clampedX - 0.5) * 2 * maxTilt;
    this.targetRotateX = (0.5 - clampedY) * 2 * maxTilt;
    this.tiltActive = true;
    this.startTiltLoop();
  }

  onTiltEnter(): void {
    if (!this.tiltEnabled) return;
    this.tiltActive = true;
  }

  onTiltLeave(): void {
    this.tiltActive = false;
    this.targetRotateX = 0;
    this.targetRotateY = 0;
    this.startTiltLoop();
  }

  flip(): void {
    this.flipped = !this.flipped;
  }

  @HostListener('window:blur')
  onWindowBlur(): void {
    this.onTiltLeave();
  }

  hideData(): void {
    this.virtualCardApi.clearUnlock();
    this.reveal = null;
    this.back.emit();
  }

  async copy(value: string, label: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(value.replace(/\s/g, ''));
      this.toast.show(`${label} copiado`, 'success');
    } catch {
      this.toast.show('No se pudo copiar', 'error');
    }
  }

  async togglePause(): Promise<void> {
    if (this.isCancelled) return;
    this.busy = true;
    try {
      const next = this.card.status === 'PAUSED' ? 'ACTIVE' : 'PAUSED';
      const updated = await this.virtualCardApi.updateStatus(this.card.id, next);
      this.card = updated;
      this.cardUpdated.emit(updated);
      this.toast.show(next === 'PAUSED' ? 'Tarjeta pausada' : 'Tarjeta reactivada', 'success');
      void this.loadAudit();
    } catch (error) {
      this.toast.show(this.virtualCardApi.handleError(error, 'No se pudo actualizar el estado'), 'error');
    } finally {
      this.busy = false;
    }
  }

  askCancelCard(): void {
    if (this.isCancelled || this.busy) return;
    this.confirmingCancel = true;
  }

  dismissCancelConfirm(): void {
    if (this.busy) return;
    this.confirmingCancel = false;
  }

  async confirmCancelCard(): Promise<void> {
    if (this.isCancelled || this.busy) return;
    this.busy = true;
    try {
      const updated = await this.virtualCardApi.cancel(this.card.id);
      this.card = updated;
      this.reveal = null;
      this.confirmingCancel = false;
      this.cardUpdated.emit(updated);
      this.toast.show('Tarjeta dada de baja', 'success');
      void this.loadAudit();
    } catch (error) {
      this.toast.show(this.virtualCardApi.handleError(error, 'No se pudo dar de baja'), 'error');
    } finally {
      this.busy = false;
    }
  }

  async reissueCard(): Promise<void> {
    this.busy = true;
    try {
      const updated = await this.virtualCardApi.reissue(this.card.id);
      this.card = updated;
      this.cardUpdated.emit(updated);
      this.toast.show('Nueva prepaga emitida', 'success');
      this.virtualCardApi.clearUnlock();
      this.back.emit();
    } catch (error) {
      this.toast.show(this.virtualCardApi.handleError(error, 'No se pudo emitir una nueva'), 'error');
    } finally {
      this.busy = false;
    }
  }

  startEditLimit(): void {
    this.limitDraft = this.card.dailyLimit;
    this.editingLimit = true;
  }

  cancelEditLimit(): void {
    this.editingLimit = false;
  }

  async saveLimit(): Promise<void> {
    if (this.limitDraft < 0) {
      this.toast.show('El límite no puede ser negativo', 'error');
      return;
    }
    this.busy = true;
    try {
      const updated = await this.virtualCardApi.updateLimit(this.card.id, this.limitDraft);
      this.card = updated;
      if (this.reveal) {
        this.reveal = { ...this.reveal, dailyLimit: updated.dailyLimit };
      }
      this.cardUpdated.emit(updated);
      this.editingLimit = false;
      this.toast.show('Límite actualizado', 'success');
      void this.loadAudit();
    } catch (error) {
      this.toast.show(this.virtualCardApi.handleError(error, 'No se pudo actualizar el límite'), 'error');
    } finally {
      this.busy = false;
    }
  }

  auditLabel(type: string): string {
    const map: Record<string, string> = {
      PIN_SET: 'PIN configurado',
      PIN_FAIL: 'PIN incorrecto',
      UNLOCK: 'Desbloqueo',
      REVEAL: 'Datos vistos',
      PAUSE: 'Pausada',
      RESUME: 'Reactivada',
      LIMIT_CHANGE: 'Límite cambiado',
      CANCEL: 'Dada de baja',
      REISSUE: 'Nueva emitida',
    };
    return map[type] || type;
  }

  private startTiltLoop(): void {
    if (this.animating) return;
    this.animating = true;
    const tick = () => {
      const ease = this.tiltActive ? 0.18 : 0.12;
      this.currentRotateX += (this.targetRotateX - this.currentRotateX) * ease;
      this.currentRotateY += (this.targetRotateY - this.currentRotateY) * ease;

      const settled =
        Math.abs(this.targetRotateX - this.currentRotateX) < 0.05 &&
        Math.abs(this.targetRotateY - this.currentRotateY) < 0.05;

      if (settled && !this.tiltActive) {
        this.currentRotateX = 0;
        this.currentRotateY = 0;
        this.applyTiltVars();
        this.animating = false;
        this.rafId = 0;
        return;
      }

      this.applyTiltVars();
      this.rafId = requestAnimationFrame(tick);
    };
    this.rafId = requestAnimationFrame(tick);
  }

  private applyTiltVars(): void {
    const el = this.tiltShell?.nativeElement;
    if (!el) return;
    el.style.setProperty('--rotate-x', `${this.currentRotateY.toFixed(2)}deg`);
    el.style.setProperty('--rotate-y', `${this.currentRotateX.toFixed(2)}deg`);
  }

  private async loadSensitive(): Promise<void> {
    if (this.isCancelled) {
      this.reveal = null;
      this.revealError = false;
      this.loadingReveal = false;
      return;
    }
    if (!this.virtualCardApi.isUnlocked()) {
      this.revealError = true;
      this.loadingReveal = false;
      return;
    }
    this.loadingReveal = true;
    this.revealError = false;
    try {
      this.reveal = await this.virtualCardApi.reveal(this.card.id);
    } catch (error) {
      logger.error('Reveal falló', error);
      this.revealError = true;
      this.toast.show('Sesión expirada. Volvé a ingresar el PIN.', 'error');
      this.virtualCardApi.clearUnlock();
      this.back.emit();
    } finally {
      this.loadingReveal = false;
    }
  }

  private async loadAudit(): Promise<void> {
    try {
      this.audit = await this.virtualCardApi.getAudit();
    } catch {
      this.audit = [];
    }
  }
}
