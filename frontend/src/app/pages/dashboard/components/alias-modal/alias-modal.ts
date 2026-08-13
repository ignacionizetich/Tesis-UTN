import {
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModalService } from '../../../../services/modal/modal.service';

@Component({
  selector: 'app-alias-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alias-modal.html',
  styleUrls: ['./alias-modal.css'],
})
export class AliasModalComponent {
  @Input() alias = '';
  @Input() cvu = '';
  @Input() currency: 'ARS' | 'USD' | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() copy = new EventEmitter<{ value: string; type: string }>();
  @Output() backdropClick = new EventEmitter<MouseEvent>();

  constructor(private modalService: ModalService) {}

  get currencyLabel(): string {
    if (this.currency === 'USD') {
      return 'dólares (USD)';
    }
    if (this.currency === 'ARS') {
      return 'pesos (ARS)';
    }
    return 'tu cuenta';
  }

  get formattedCvu(): string {
    const digits = (this.cvu || '').replace(/\s/g, '');
    if (digits.length !== 22) {
      return this.cvu;
    }
    // CVU: 000 + bloque entidad + resto, agrupado para lectura
    return digits.replace(/(\d{3})(\d{4})(\d{4})(\d{4})(\d{4})(\d{3})/, '$1 $2 $3 $4 $5 $6');
  }

  close(): void {
    this.modalService.closeModal();
    this.closed.emit();
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('alias-modal')) {
      this.close();
    }
  }

  copyAlias(): void {
    this.copy.emit({ value: this.alias, type: 'Alias' });
  }

  copyCvu(): void {
    this.copy.emit({ value: this.cvu, type: 'CVU' });
  }
}
