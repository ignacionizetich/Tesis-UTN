import {
  Component,
  EventEmitter,
  OnInit,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { QRCodeComponent } from 'angularx-qrcode';
import qrData from '../../../../models/qrData';
import { QrApi } from '../../../../services/qr-api/qr.api';
import { SessionStore } from '../../../../core/session/session.store';
import { ModalService } from '../../../../services/modal/modal.service';
import { formatDni } from '../../../../shared/utils/dni-format';
import { logger } from '../../../../shared/utils/logger';

@Component({
  selector: 'app-receive-qr-modal',
  standalone: true,
  imports: [CommonModule, QRCodeComponent],
  templateUrl: './receive-qr-modal.html',
  styleUrls: ['./receive-qr-modal.css'],
})
export class ReceiveQrModalComponent implements OnInit {
  isLoadingQr = true;
  loadError = false;
  qrCodeDataString: string | null = null;
  qrCodeDataObject: qrData | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() backdropClick = new EventEmitter<MouseEvent>();

  constructor(
    private qrApi: QrApi,
    private sessionStore: SessionStore,
    private modalService: ModalService
  ) {}

  get currencyLabel(): string {
    const currency = this.qrCodeDataObject?.currency;
    return currency === 'USD' ? 'dólares (USD)' : 'pesos (ARS)';
  }

  get formattedDni(): string {
    return formatDni(this.qrCodeDataObject?.dni);
  }

  ngOnInit(): void {
    const accountId = this.sessionStore.getAccountId();
    if (!accountId) {
      logger.error('No se encontro el ID de la cuenta en la sesión.');
      this.close();
      return;
    }

    const accountIdNumber = parseInt(accountId, 10);
    this.isLoadingQr = true;
    this.loadError = false;

    this.qrApi.getMyQrData(accountIdNumber).subscribe({
      next: (data) => {
        this.qrCodeDataObject = data;
        this.qrCodeDataString = JSON.stringify(data);
        this.isLoadingQr = false;
      },
      error: (err) => {
        logger.error('Error al obtener los datos del QR', err);
        this.isLoadingQr = false;
        this.loadError = true;
      },
    });
  }

  close(): void {
    this.isLoadingQr = false;
    this.modalService.closeModal();
    this.closed.emit();
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('qr-modal')) {
      this.close();
    }
  }
}
