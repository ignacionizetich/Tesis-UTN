import {
  Component,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { ModalService } from '../../../../services/modal/modal.service';
import { VirtualCardApi } from '../../../../services/virtual-card/virtual-card.api';
import { ToastService } from '../../../../services/toast/toast.service';
import { VirtualCardSummary } from '../../../../models/virtual-card';
import { CardsListModalComponent } from './components/cards-list-modal/cards-list-modal';
import { CardPinModalComponent } from './components/card-pin-modal/card-pin-modal';
import { CardDetailModalComponent } from './components/card-detail-modal/card-detail-modal';
import { logger } from '../../../../shared/utils/logger';

@Component({
  selector: 'app-cards-modals',
  standalone: true,
  imports: [
    CommonModule,
    CardsListModalComponent,
    CardPinModalComponent,
    CardDetailModalComponent,
  ],
  templateUrl: './cards-modals.html',
})
export class CardsModalsComponent implements OnInit, OnDestroy {
  currentModal: string | null = null;
  cards: VirtualCardSummary[] = [];
  pinConfigured = false;
  selectedCard: VirtualCardSummary | null = null;
  loading = false;

  private subscriptions: Subscription[] = [];

  constructor(
    private modalService: ModalService,
    private virtualCardApi: VirtualCardApi,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.subscriptions.push(
      this.modalService.modalState$.subscribe((state) => {
        this.currentModal = state.currentModal;
        if (state.currentModal === 'cards') {
          void this.loadCards();
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((s) => s.unsubscribe());
  }

  async loadCards(): Promise<void> {
    this.loading = true;
    try {
      const response = await this.virtualCardApi.listCards();
      this.cards = response.cards || [];
      this.pinConfigured = response.pinConfigured;
    } catch (error) {
      logger.error('Error cargando tarjetas', error);
      this.toast.show(this.virtualCardApi.handleError(error, 'No se pudieron cargar las tarjetas'), 'error');
    } finally {
      this.loading = false;
    }
  }

  closeAll(): void {
    this.selectedCard = null;
    this.modalService.closeModal();
  }

  onSelectCard(card: VirtualCardSummary): void {
    this.selectedCard = card;
    // Baja: no hace falta PIN para ver estado / solicitar nueva
    if (card.status === 'CANCELLED') {
      this.modalService.openModal('cardDetail');
      return;
    }
    if (this.virtualCardApi.isUnlocked()) {
      this.modalService.openModal('cardDetail');
      return;
    }
    this.modalService.openModal('cardPin');
  }

  onPinUnlocked(): void {
    this.modalService.openModal('cardDetail');
  }

  onBackToList(): void {
    this.selectedCard = null;
    this.modalService.openModal('cards');
    void this.loadCards();
  }

  onCardUpdated(card: VirtualCardSummary): void {
    this.selectedCard = card;
    this.cards = this.cards.map((c) => (c.id === card.id ? card : c));
    if (card.status === 'CANCELLED') {
      this.virtualCardApi.clearUnlock();
    }
  }

  async onReissueFromList(card: VirtualCardSummary): Promise<void> {
    try {
      const updated = await this.virtualCardApi.reissue(card.id);
      this.toast.show('Nueva prepaga emitida', 'success');
      this.cards = this.cards.map((c) => (c.id === updated.id ? updated : c));
    } catch (error) {
      this.toast.show(this.virtualCardApi.handleError(error, 'No se pudo emitir una nueva'), 'error');
    }
  }
}
