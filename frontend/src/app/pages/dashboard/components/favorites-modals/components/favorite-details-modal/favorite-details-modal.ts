import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FavoriteContact } from '../../../../../../models/favorite-contact';

@Component({
  selector: 'app-favorite-details-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './favorite-details-modal.html',
  styleUrl: './favorite-details-modal.css',
})
export class FavoriteDetailsModalComponent {
  @Input({ required: true }) favorite!: FavoriteContact;

  @Output() closed = new EventEmitter<void>();
  @Output() back = new EventEmitter<void>();
  @Output() transfer = new EventEmitter<FavoriteContact>();
  @Output() edit = new EventEmitter<FavoriteContact>();
  @Output() remove = new EventEmitter<FavoriteContact>();
  @Output() backdropClick = new EventEmitter<MouseEvent>();

  get accountTypeLabel(): string {
    const type = (this.favorite?.accountType || '').toUpperCase();
    if (type.includes('USD') || type === 'DOLAR' || type === 'DÓLAR') {
      return 'Cuenta USD';
    }
    if (type.includes('ARS') || type.includes('PESO')) {
      return 'Cuenta ARS';
    }
    return this.favorite?.accountType || 'Cuenta';
  }

  get formattedCvu(): string {
    const digits = (this.favorite?.accountCbu || '').replace(/\s/g, '');
    if (digits.length !== 22) {
      return this.favorite?.accountCbu || '—';
    }
    return digits.replace(
      /(\d{3})(\d{4})(\d{4})(\d{4})(\d{4})(\d{3})/,
      '$1 $2 $3 $4 $5 $6'
    );
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('fav-details-modal')) {
      this.backdropClick.emit(event);
    }
  }

  initials(name: string): string {
    const parts = (name || '').trim().split(/\s+/);
    return `${parts[0]?.charAt(0) || ''}${parts[1]?.charAt(0) || ''}`.toUpperCase();
  }
}
