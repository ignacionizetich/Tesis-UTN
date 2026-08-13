import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FavoriteContact } from '../../../../../../models/favorite-contact';

@Component({
  selector: 'app-favorite-details-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './favorite-details-modal.html',
  styleUrls: ['../../../../styles/modals-shared.css', '../../../../styles/favorites.css'],
})
export class FavoriteDetailsModalComponent {
  @Input({ required: true }) favorite!: FavoriteContact;

  @Output() closed = new EventEmitter<void>();
  @Output() back = new EventEmitter<void>();
  @Output() transfer = new EventEmitter<FavoriteContact>();
  @Output() edit = new EventEmitter<FavoriteContact>();
  @Output() remove = new EventEmitter<FavoriteContact>();
  @Output() backdropClick = new EventEmitter<MouseEvent>();

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
