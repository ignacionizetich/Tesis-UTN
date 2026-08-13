import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FavoriteContact } from '../../../../../../models/favorite-contact';

@Component({
  selector: 'app-favorites-list-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './favorites-list-modal.html',
  styleUrls: ['../../../../styles/modals-shared.css', '../../../../styles/favorites.css'],
})
export class FavoritesListModalComponent {
  @Input() favorites: FavoriteContact[] = [];

  @Output() closed = new EventEmitter<void>();
  @Output() favoriteSelected = new EventEmitter<FavoriteContact>();
  @Output() backdropClick = new EventEmitter<MouseEvent>();

  trackFavorite(index: number, favorite: FavoriteContact): string {
    return `${index}_${favorite.id}_${favorite.contactAlias}`;
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal')) {
      this.backdropClick.emit(event);
    }
  }

  initials(name: string): string {
    const parts = (name || '').trim().split(/\s+/);
    return `${parts[0]?.charAt(0) || ''}${parts[1]?.charAt(0) || ''}`;
  }
}
