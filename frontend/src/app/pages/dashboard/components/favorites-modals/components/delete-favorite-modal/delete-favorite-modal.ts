import {
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FavoriteService } from '../../../../../../services/favorite/favorite.service';
import { ToastService } from '../../../../../../services/toast/toast.service';
import { FavoriteContact } from '../../../../../../models/favorite-contact';
import { errorMessage } from '../../../../../../shared/utils/error-message';
import { logger } from '../../../../../../shared/utils/logger';

@Component({
  selector: 'app-delete-favorite-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './delete-favorite-modal.html',
  styleUrls: ['../../../../styles/modals-shared.css', '../../../../styles/favorites.css'],
})
export class DeleteFavoriteModalComponent {
  @Input({ required: true }) favorite!: FavoriteContact;

  @Output() closed = new EventEmitter<void>();
  @Output() deleted = new EventEmitter<void>();
  @Output() backdropClick = new EventEmitter<MouseEvent>();

  isDeletingFavorite = false;

  constructor(
    private favoriteService: FavoriteService,
    private toast: ToastService
  ) {}

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal')) {
      this.backdropClick.emit(event);
    }
  }

  initials(name: string): string {
    const parts = (name || '').trim().split(/\s+/);
    return `${parts[0]?.charAt(0) || ''}${parts[1]?.charAt(0) || ''}`;
  }

  async confirm(): Promise<void> {
    if (!this.favorite) {
      return;
    }

    this.isDeletingFavorite = true;

    try {
      await this.favoriteService.removeFavoriteContact(
        this.favorite.id,
        this.favorite.contactAlias
      );

      this.toast.show('Contacto eliminado de favoritos', 'success');
      this.deleted.emit();
    } catch (error: unknown) {
      logger.error('Error eliminando favorito:', error);
      this.toast.show(errorMessage(error, 'Error al eliminar el contacto'), 'error');
    } finally {
      this.isDeletingFavorite = false;
    }
  }
}
