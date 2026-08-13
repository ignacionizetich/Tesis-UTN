import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserResponse } from '../../../models/admin.interface';
import { formatDni as formatDniAr } from '../../../shared/utils/dni-format';

@Component({
  selector: 'app-authenticated-info',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './authenticated-info.html',
  styleUrls: ['./authenticated-info.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthenticatedInfoComponent {
  @Input() user: UserResponse | null = null;
  @Input() showModal = false;
  @Output() closeModal = new EventEmitter<void>();
  @Output() toggleUserStatus = new EventEmitter<UserResponse>();

  formatDni(dni: string | number | null | undefined): string {
    return formatDniAr(dni);
  }

  initials(user: UserResponse): string {
    const a = (user.name || '').trim().charAt(0);
    const b = (user.lastName || '').trim().charAt(0);
    return `${a}${b}`.toUpperCase() || '?';
  }

  onCloseModal(): void {
    this.closeModal.emit();
  }

  onToggleUserStatus(): void {
    if (this.user) {
      this.toggleUserStatus.emit(this.user);
    }
  }

  onBackdropClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (target.classList.contains('user-detail-modal')) {
      this.onCloseModal();
    }
  }
}
