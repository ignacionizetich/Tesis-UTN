import {
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-alias-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alias-modal.html',
  styleUrls: ['../../styles/modals-shared.css'],
})
export class AliasModalComponent {
  @Input() alias = '';
  @Input() cvu = '';

  @Output() closed = new EventEmitter<void>();
  @Output() copy = new EventEmitter<{ value: string; type: string }>();
  @Output() backdropClick = new EventEmitter<MouseEvent>();
}
