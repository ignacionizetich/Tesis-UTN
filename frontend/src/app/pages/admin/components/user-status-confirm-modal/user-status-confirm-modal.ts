import {
  Component,
  EventEmitter,
  Input,
  Output,
  ViewEncapsulation,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserResponse } from '../../../../models/admin.interface';

@Component({
  selector: 'app-user-status-confirm-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-status-confirm-modal.html',
  styleUrls: ['../../admin.css'],
  encapsulation: ViewEncapsulation.None,
})
export class UserStatusConfirmModalComponent {
  @Input() user: UserResponse | null = null;
  @Input() visible = false;
  @Input() loading = false;

  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();
}
