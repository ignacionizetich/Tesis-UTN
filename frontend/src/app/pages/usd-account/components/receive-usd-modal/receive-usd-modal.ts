import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../../../services/toast-service/toast.service';

@Component({
  selector: 'app-receive-usd-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './receive-usd-modal.html',
  styleUrls: ['../../styles/usd-modals.css'],
})
export class ReceiveUsdModalComponent {
  @Input() alias = '';
  @Input() cvu = '';

  @Output() closed = new EventEmitter<void>();

  constructor(private toast: ToastService) {}

  copyToClipboard(text: string, label: string): void {
    navigator.clipboard.writeText(text).then(() => {
      this.toast.show(`${label} copiado al portapapeles`, 'success');
    }).catch(() => {
      this.toast.show('Error al copiar', 'error');
    });
  }
}
