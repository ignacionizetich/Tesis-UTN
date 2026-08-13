import {
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FavoriteService } from '../../../../../../services/favorite/favorite.service';
import { ToastService } from '../../../../../../services/toast/toast.service';
import { TransferApi } from '../../../../../../services/transfer-api/transfer.api';
import { UserDataStore } from '../../../../../../services/user-data-store/user-data.store';
import { TransferFlowService } from '../../../../../../services/transfer-flow/transfer-flow.service';
import { TransferData } from '../../../../../../models/transfer.interface';
import { errorMessage } from '../../../../../../shared/utils/error-message';
import { logger } from '../../../../../../shared/utils/logger';

export type TransferCompletedData = TransferData & { idaccount: string | number };

@Component({
  selector: 'app-add-favorite-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './add-favorite-modal.html',
  styleUrls: ['./add-favorite-modal.css'],
})
export class AddFavoriteModalComponent {
  @Input() transferCompletedData: TransferCompletedData | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();
  @Output() backdropClick = new EventEmitter<MouseEvent>();

  favoriteContactAlias = '';
  favoriteContactDescription = '';
  isAddingFavorite = false;

  constructor(
    private favoriteService: FavoriteService,
    private toast: ToastService,
    private transferApi: TransferApi,
    private userDataStore: UserDataStore,
    private transferFlow: TransferFlowService
  ) {}

  get recipientName(): string {
    const data = this.transferCompletedData;
    if (!data) {
      return '';
    }
    const name = `${data.user?.nombre || ''} ${data.user?.apellido || ''}`.trim();
    return name || data.alias || 'Contacto';
  }

  get recipientAlias(): string {
    return this.transferCompletedData?.alias || '';
  }

  get recipientInitials(): string {
    const data = this.transferCompletedData;
    if (!data?.user) {
      return (this.recipientAlias.charAt(0) || '?').toUpperCase();
    }
    const a = (data.user.nombre || '?').charAt(0);
    const b = (data.user.apellido || '').charAt(0);
    return `${a}${b}`.toUpperCase();
  }

  get canSave(): boolean {
    return !!this.favoriteContactAlias.trim() && !this.isAddingFavorite;
  }

  close(): void {
    if (this.isAddingFavorite) {
      return;
    }
    this.closed.emit();
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('add-fav-modal')) {
      this.backdropClick.emit(event);
    }
  }

  resetForm(): void {
    this.favoriteContactAlias = '';
    this.favoriteContactDescription = '';
  }

  async save(): Promise<void> {
    if (!this.favoriteContactAlias.trim()) {
      this.toast.show('Por favor ingresá un nombre para el contacto', 'error');
      return;
    }

    if (!this.transferCompletedData) {
      this.toast.show('Error: datos de transferencia no disponibles', 'error');
      return;
    }

    this.isAddingFavorite = true;

    try {
      if (!this.transferCompletedData.idaccount) {
        this.toast.show('Error: ID de cuenta no disponible', 'error');
        return;
      }

      let accountId: number;
      const rawId = this.transferCompletedData.idaccount;

      if (typeof rawId === 'number') {
        accountId = rawId;
      } else {
        accountId = parseInt(String(rawId), 10);

        if (isNaN(accountId)) {
          const searchTerm =
            this.transferCompletedData.cvu || this.transferCompletedData.alias;
          if (!searchTerm) {
            this.toast.show('Error: No se puede identificar la cuenta', 'error');
            return;
          }

          const accountData = await this.transferApi.buscarCuenta(searchTerm);
          accountId = parseInt(accountData.idaccount, 10);

          if (isNaN(accountId)) {
            this.toast.show('Error: No se pudo obtener el ID de cuenta', 'error');
            return;
          }
        }
      }

      const isAlreadyFavorite = await this.transferFlow.isFavorite(
        accountId,
        this.transferCompletedData.cvu
      );

      const currentUser = this.userDataStore.getCurrent();
      const currentUserId = parseInt(currentUser?.idAccount || '0', 10);

      if (currentUserId === accountId) {
        this.toast.show('No puedes agregarte a ti mismo como favorito', 'error');
        return;
      }

      if (isAlreadyFavorite) {
        this.toast.show('Esta cuenta ya está en tus favoritos', 'error');
        return;
      }

      await this.favoriteService.addFavoriteContact(
        accountId,
        this.favoriteContactAlias.trim(),
        this.favoriteContactDescription.trim() || undefined
      );

      this.toast.show('Contacto agregado a favoritos', 'success');
      this.resetForm();
      this.saved.emit();
    } catch (error: unknown) {
      logger.error('Error agregando a favoritos:', error);
      this.toast.show(errorMessage(error, 'Error al agregar el contacto a favoritos'), 'error');
    } finally {
      this.isAddingFavorite = false;
    }
  }
}
