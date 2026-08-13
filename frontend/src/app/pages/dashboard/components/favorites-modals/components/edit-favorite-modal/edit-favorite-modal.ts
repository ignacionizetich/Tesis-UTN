import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FavoriteService } from '../../../../../../services/favorite/favorite.service';
import { ToastService } from '../../../../../../services/toast/toast.service';
import { FavoriteContact } from '../../../../../../models/favorite-contact';
import { errorMessage } from '../../../../../../shared/utils/error-message';
import { logger } from '../../../../../../shared/utils/logger';

@Component({
  selector: 'app-edit-favorite-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './edit-favorite-modal.html',
  styleUrls: ['./edit-favorite-modal.css'],
})
export class EditFavoriteModalComponent implements OnChanges {
  @Input({ required: true }) favorite!: FavoriteContact;

  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();
  @Output() backdropClick = new EventEmitter<MouseEvent>();

  favoriteContactAlias = '';
  favoriteContactDescription = '';
  isUpdatingFavorite = false;

  constructor(
    private favoriteService: FavoriteService,
    private toast: ToastService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['favorite'] && this.favorite) {
      this.favoriteContactAlias = this.favorite.contactAlias;
      this.favoriteContactDescription = this.favorite.description || '';
    }
  }

  get contactInitials(): string {
    const name = (this.favorite?.accountOwnerName || this.favorite?.contactAlias || '?').trim();
    const parts = name.split(/\s+/).filter(Boolean);
    if (parts.length >= 2) {
      return `${parts[0].charAt(0)}${parts[1].charAt(0)}`.toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  }

  get canSave(): boolean {
    return !!this.favoriteContactAlias.trim() && !this.isUpdatingFavorite;
  }

  close(): void {
    if (this.isUpdatingFavorite) {
      return;
    }
    this.closed.emit();
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('edit-fav-modal')) {
      if (this.isUpdatingFavorite) {
        return;
      }
      this.backdropClick.emit(event);
    }
  }

  async save(): Promise<void> {
    if (!this.favoriteContactAlias.trim()) {
      this.toast.show('Por favor ingresá un nombre para el contacto', 'error');
      return;
    }

    if (!this.favorite) {
      this.toast.show('Error: contacto no seleccionado', 'error');
      return;
    }

    this.isUpdatingFavorite = true;

    try {
      await this.favoriteService.updateFavoriteContact(
        this.favorite.id,
        this.favoriteContactAlias.trim(),
        this.favoriteContactDescription.trim() || undefined
      );

      this.toast.show('Contacto actualizado correctamente', 'success');
      this.saved.emit();
    } catch (error: unknown) {
      logger.error('Error updating favorite contact:', error);
      this.toast.show(errorMessage(error, 'Error al actualizar el contacto'), 'error');
    } finally {
      this.isUpdatingFavorite = false;
    }
  }
}
