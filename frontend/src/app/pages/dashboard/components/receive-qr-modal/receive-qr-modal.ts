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
import { SessionStore } from '../../../../core/session/session-store';
import { ModalService } from '../../../../services/modal-service/modal-service';

@Component({
  selector: 'app-receive-qr-modal',
  standalone: true,
  imports: [CommonModule, QRCodeComponent],
  templateUrl: './receive-qr-modal.html',
  styleUrls: ['../../styles/modals-shared.css', '../../styles/receive-qr.css'],
})
export class ReceiveQrModalComponent implements OnInit {
  isLoadingQr = true;
  qrCodeDataString: string | null = null;
  qrCodeDataObject: qrData | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() backdropClick = new EventEmitter<MouseEvent>();

  constructor(
    private qrApi: QrApi,
    private sessionStore: SessionStore,
    private modalService: ModalService
  ) {}

  ngOnInit(): void {
    const accountId = this.sessionStore.getAccountId();
    if (!accountId) {
      console.error('No se encontro el ID de la cuenta en la sesión.');
      this.close();
      return;
    }

    const accountIdNumber = parseInt(accountId, 10);
    this.isLoadingQr = true;

    this.qrApi.getMyQrData(accountIdNumber).subscribe({
      next: (data) => {
        this.qrCodeDataObject = data;
        this.qrCodeDataString = JSON.stringify(data);
        this.isLoadingQr = false;
      },
      error: (err) => {
        console.error('Error al obtener los datos del QR', err);
        this.isLoadingQr = false;
        this.close();
      },
    });
  }

  close(): void {
    this.isLoadingQr = false;
    this.modalService.closeModal();
    this.closed.emit();
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal')) {
      this.close();
    }
  }
}
