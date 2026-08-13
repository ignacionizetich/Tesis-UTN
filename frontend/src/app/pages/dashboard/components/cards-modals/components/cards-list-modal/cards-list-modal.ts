import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { VirtualCardSummary } from '../../../../../../models/virtual-card';

@Component({
  selector: 'app-cards-list-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cards-list-modal.html',
  styleUrls: ['./cards-list-modal.css'],
})
export class CardsListModalComponent {
  @Input() cards: VirtualCardSummary[] = [];
  @Input() pinConfigured = false;
  @Input() loading = false;

  @Output() closed = new EventEmitter<void>();
  @Output() cardSelected = new EventEmitter<VirtualCardSummary>();
  @Output() reissueRequested = new EventEmitter<VirtualCardSummary>();

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('cards-list-modal')) {
      this.closed.emit();
    }
  }

  masked(last4: string): string {
    return `•••• ${last4}`;
  }

  statusLabel(card: VirtualCardSummary): string | null {
    if (card.status === 'CANCELLED') return 'Dada de baja';
    if (card.status === 'PAUSED') return 'Pausada';
    if (card.expired) return 'Vencida';
    return null;
  }

  onReissueClick(event: MouseEvent, card: VirtualCardSummary): void {
    event.stopPropagation();
    this.reissueRequested.emit(card);
  }
}
