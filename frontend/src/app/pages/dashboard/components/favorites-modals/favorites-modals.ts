import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { FavoriteService } from '../../../../services/favorite/favorite.service';
import { ModalService } from '../../../../services/modal/modal.service';
import { ToastService } from '../../../../services/toast/toast.service';
import { FavoriteContact } from '../../../../models/favorite-contact';
import {
  AddFavoriteModalComponent,
  TransferCompletedData,
} from './components/add-favorite-modal/add-favorite-modal';
import { FavoritesListModalComponent } from './components/favorites-list-modal/favorites-list-modal';
import { FavoriteDetailsModalComponent } from './components/favorite-details-modal/favorite-details-modal';
import { EditFavoriteModalComponent } from './components/edit-favorite-modal/edit-favorite-modal';
import { DeleteFavoriteModalComponent } from './components/delete-favorite-modal/delete-favorite-modal';

export type { TransferCompletedData };

@Component({
  selector: 'app-favorites-modals',
  standalone: true,
  imports: [
    CommonModule,
    FavoritesListModalComponent,
    FavoriteDetailsModalComponent,
    AddFavoriteModalComponent,
    EditFavoriteModalComponent,
    DeleteFavoriteModalComponent,
  ],
  templateUrl: './favorites-modals.html',
})
export class FavoritesModalsComponent implements OnInit, OnDestroy {
  @Input() transferCompletedData: TransferCompletedData | null = null;

  @Output() transferRequested = new EventEmitter<FavoriteContact>();
  @Output() transferFlowFinished = new EventEmitter<void>();

  currentModal: string | null = null;
  favoriteContacts: FavoriteContact[] = [];
  selectedFavoriteContact: FavoriteContact | null = null;
  favoriteToDelete: FavoriteContact | null = null;

  private subscriptions: Subscription[] = [];

  constructor(
    private favoriteService: FavoriteService,
    private modalService: ModalService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.subscriptions.push(
      this.modalService.modalState$.subscribe((state) => {
        this.currentModal = state.currentModal;
      }),
      this.favoriteService.favoriteContacts$.subscribe((favorites) => {
        this.favoriteContacts = favorites;
      }),
      this.favoriteService.selectedFavorite$.subscribe((favorite) => {
        this.selectedFavoriteContact = favorite;
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach((s) => s.unsubscribe());
  }

  closeFavoritesModal(): void {
    this.modalService.closeModal();
  }

  openFavoriteDetailsModal(favorite: FavoriteContact): void {
    this.favoriteService.selectFavorite(favorite);
    this.modalService.openModal('favoriteDetails');
  }

  closeFavoriteDetailsModal(): void {
    this.modalService.closeModal();
    this.favoriteService.clearSelectedFavorite();
  }

  backToFavoritesList(): void {
    this.favoriteService.clearSelectedFavorite();
    this.modalService.openModal('favorites');
  }

  transferToFavorite(favorite: FavoriteContact): void {
    this.transferRequested.emit(favorite);
  }

  /** API pública para dashboard / transfer-wizard (ViewChild). */
  openAddFavoriteModal(): void {
    this.modalService.openModal('addFavorite');
  }

  closeAddFavoriteModal(): void {
    this.modalService.closeModal();
  }

  onAddFavoriteSaved(): void {
    this.closeAddFavoriteModal();
    this.transferFlowFinished.emit();
  }

  /** API pública para skip post-transferencia. */
  skipAddToFavorites(): void {
    this.toast.show('Transferencia realizada con éxito', 'success');
    this.closeAddFavoriteModal();
    this.transferFlowFinished.emit();
  }

  openEditFavoriteModal(favorite: FavoriteContact): void {
    this.favoriteService.selectFavorite(favorite);
    this.modalService.openModal('editFavorite');
  }

  /** Cancelar edición → volver al detalle del contacto. */
  closeEditFavoriteModal(): void {
    this.modalService.openModal('favoriteDetails');
  }

  onEditFavoriteSaved(): void {
    const selected = this.favoriteService.getSelectedFavorite();
    if (selected) {
      const updated = this.favoriteContacts.find((f) => f.id === selected.id);
      if (updated) {
        this.favoriteService.selectFavorite(updated);
      }
    }
    this.modalService.openModal('favoriteDetails');
  }

  openDeleteFavoriteModal(favorite: FavoriteContact): void {
    this.favoriteToDelete = favorite;
    this.modalService.openModal('deleteFavorite');
  }

  /** Cancelar eliminación → volver al detalle. */
  closeDeleteFavoriteModal(): void {
    this.modalService.openModal('favoriteDetails');
    this.favoriteToDelete = null;
  }

  onDeleteFavoriteDeleted(): void {
    this.favoriteToDelete = null;
    this.favoriteService.clearSelectedFavorite();
    this.modalService.openModal('favorites');
  }

  onBackdropClose(modalType: string): void {
    if (modalType === 'favoriteDetails') {
      this.closeFavoriteDetailsModal();
    } else if (modalType === 'addFavorite') {
      this.closeAddFavoriteModal();
    } else if (modalType === 'editFavorite') {
      this.closeEditFavoriteModal();
    } else if (modalType === 'deleteFavorite') {
      this.closeDeleteFavoriteModal();
    } else {
      this.modalService.closeModal();
    }
  }
}
