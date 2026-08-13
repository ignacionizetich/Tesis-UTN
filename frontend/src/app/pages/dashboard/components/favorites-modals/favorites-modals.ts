import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewEncapsulation,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { FavoriteService } from '../../../../services/favorite-service/favorite-service';
import { ModalService } from '../../../../services/modal-service/modal-service';
import { ToastService } from '../../../../services/toast-service/toast.service';
import { TransferApi } from '../../../../services/transfer-api/transfer.api';
import { UserDataStore } from '../../../../services/user-data-store/user-data.store';
import { TransferFlowService } from '../../../../services/transfer-flow/transfer-flow.service';

@Component({
  selector: 'app-favorites-modals',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './favorites-modals.html',
  styleUrls: ['../../styles/modals-shared.css', '../../styles/favorites.css'],
  encapsulation: ViewEncapsulation.None,
})
export class FavoritesModalsComponent implements OnInit, OnDestroy {
  @Input() transferCompletedData: any = null;

  @Output() transferRequested = new EventEmitter<any>();
  @Output() transferFlowFinished = new EventEmitter<void>();

  currentModal: string | null = null;
  favoriteContacts: any[] = [];
  selectedFavoriteContact: any = null;
  favoriteContactAlias = '';
  favoriteContactDescription = '';
  isUpdatingFavorite = false;
  isAddingFavorite = false;
  isDeletingFavorite = false;
  favoriteToDelete: any = null;

  private subscriptions: Subscription[] = [];

  constructor(
    private favoriteService: FavoriteService,
    private modalService: ModalService,
    private toast: ToastService,
    private transferApi: TransferApi,
    private userDataStore: UserDataStore,
    private transferFlow: TransferFlowService
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

  trackFavorite(index: number, favorite: any): string {
    return `${index}_${favorite.id}_${favorite.contactAlias}`;
  }

  onModalBackdropClick(event: MouseEvent, modalType: string): void {
    if ((event.target as HTMLElement).classList.contains('modal')) {
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

  closeFavoritesModal(): void {
    this.modalService.closeModal();
  }

  openFavoriteDetailsModal(favorite: any): void {
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

  transferToFavorite(favorite: any): void {
    this.transferRequested.emit(favorite);
  }

  openAddFavoriteModal(): void {
    this.favoriteContactAlias = '';
    this.favoriteContactDescription = '';
    this.modalService.openModal('addFavorite');
  }

  closeAddFavoriteModal(): void {
    this.modalService.closeModal();
    this.favoriteContactAlias = '';
    this.favoriteContactDescription = '';
  }

  async addToFavorites(): Promise<void> {
    if (!this.favoriteContactAlias.trim()) {
      this.toast.show('Por favor ingresa un nombre para el contacto', 'error');
      return;
    }

    if (!this.transferCompletedData) {
      this.toast.show('Error: datos de transferencia no disponibles', 'error');
      return;
    }

    this.isAddingFavorite = true;

    try {
      if (!this.transferCompletedData.idaccount) {
        this.toast.show('Error: ID de cuenta no disponible', 'error');
        return;
      }

      let accountId: number;

      if (typeof this.transferCompletedData.idaccount === 'number') {
        accountId = this.transferCompletedData.idaccount;
      } else {
        accountId = parseInt(this.transferCompletedData.idaccount.toString(), 10);

        if (isNaN(accountId)) {
          const searchTerm =
            this.transferCompletedData.cvu || this.transferCompletedData.alias;
          if (!searchTerm) {
            this.toast.show('Error: No se puede identificar la cuenta', 'error');
            return;
          }

          const accountData = await this.transferApi.buscarCuenta(searchTerm);
          accountId = parseInt(accountData.idaccount, 10);

          if (isNaN(accountId)) {
            this.toast.show('Error: No se pudo obtener el ID de cuenta', 'error');
            return;
          }
        }
      }

      const isAlreadyFavorite = await this.transferFlow.isFavorite(
        accountId,
        this.transferCompletedData?.cvu
      );

      const currentUser = this.userDataStore.getCurrent();
      const currentUserId = parseInt(currentUser?.idAccount || '0', 10);

      if (currentUserId === accountId) {
        this.toast.show('No puedes agregarte a ti mismo como favorito', 'error');
        return;
      }

      if (isAlreadyFavorite) {
        this.toast.show('Esta cuenta ya está en tus favoritos', 'error');
        return;
      }

      await this.favoriteService.addFavoriteContact(
        accountId,
        this.favoriteContactAlias.trim(),
        this.favoriteContactDescription.trim() || undefined
      );

      this.toast.show('Contacto agregado a favoritos', 'success');
      this.closeAddFavoriteModal();
      this.transferFlowFinished.emit();
    } catch (error: any) {
      console.error('Error agregando a favoritos:', error);
      this.toast.show(error?.message || 'Error al agregar el contacto a favoritos', 'error');
    } finally {
      this.isAddingFavorite = false;
    }
  }

  skipAddToFavorites(): void {
    this.toast.show('Transferencia realizada con éxito', 'success');
    this.closeAddFavoriteModal();
    this.transferFlowFinished.emit();
  }

  openEditFavoriteModal(favorite: any): void {
    this.favoriteService.selectFavorite(favorite);
    this.favoriteContactAlias = favorite.contactAlias;
    this.favoriteContactDescription = favorite.description || '';
    this.modalService.closeModal();
    this.modalService.openModal('editFavorite');
  }

  closeEditFavoriteModal(): void {
    this.modalService.closeModal();
    this.favoriteService.clearSelectedFavorite();
    this.favoriteContactAlias = '';
    this.favoriteContactDescription = '';
  }

  async updateFavoriteContact(): Promise<void> {
    if (!this.favoriteContactAlias.trim()) {
      this.toast.show('Por favor ingresa un nombre para el contacto', 'error');
      return;
    }

    if (!this.selectedFavoriteContact) {
      this.toast.show('Error: contacto no seleccionado', 'error');
      return;
    }

    this.isUpdatingFavorite = true;

    try {
      await this.favoriteService.updateFavoriteContact(
        this.selectedFavoriteContact.id,
        this.favoriteContactAlias.trim(),
        this.favoriteContactDescription.trim() || undefined
      );

      this.toast.show('Contacto actualizado correctamente', 'success');
      this.closeEditFavoriteModal();
    } catch (error: any) {
      console.error('Error updating favorite contact:', error);
      this.toast.show(error?.message || 'Error al actualizar el contacto', 'error');
    } finally {
      this.isUpdatingFavorite = false;
    }
  }

  openDeleteFavoriteModal(favorite: any): void {
    this.favoriteToDelete = favorite;
    this.isDeletingFavorite = false;
    this.modalService.openModal('deleteFavorite');
  }

  closeDeleteFavoriteModal(): void {
    this.modalService.closeModal();
    this.favoriteToDelete = null;
    this.isDeletingFavorite = false;
  }

  async confirmDeleteFavorite(): Promise<void> {
    if (!this.favoriteToDelete) {
      return;
    }

    this.isDeletingFavorite = true;

    try {
      await this.favoriteService.removeFavoriteContact(
        this.favoriteToDelete.id,
        this.favoriteToDelete.contactAlias
      );

      this.toast.show('Contacto eliminado de favoritos', 'success');
      this.closeDeleteFavoriteModal();
    } catch (error: any) {
      console.error('Error eliminando favorito:', error);
      this.toast.show(error?.message || 'Error al eliminar el contacto', 'error');
    } finally {
      this.isDeletingFavorite = false;
    }
  }
}
