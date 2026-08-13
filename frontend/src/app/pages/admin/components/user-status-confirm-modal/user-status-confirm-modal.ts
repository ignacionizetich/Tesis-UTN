import {
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserResponse } from '../../../../models/admin.interface';

@Component({
  selector: 'app-user-status-confirm-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-status-confirm-modal.html',
  styleUrls: ['../../styles/users-list.css', '../../styles/confirm-modal.css'],
})
export class UserStatusConfirmModalComponent {
  @Input() user: UserResponse | null = null;
  @Input() visible = false;
  @Input() loading = false;

  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();
}
