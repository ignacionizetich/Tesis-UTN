import {
  Component,
  EventEmitter,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import UserData from '../../../../models/user-data';
import { UserDataStore } from '../../../../services/user-data-store/user-data.store';
import { ToastService } from '../../../../services/toast/toast.service';
import { ModalService } from '../../../../services/modal/modal.service';
import { formatDni as formatDniAr } from '../../../../shared/utils/dni-format';
import { logger } from '../../../../shared/utils/logger';

@Component({
  selector: 'app-profile-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile-modal.html',
  styleUrls: ['./profile-modal.css'],
})
export class ProfileModalComponent implements OnInit, OnDestroy {
  userData: UserData = {
    name: '',
    lastName: '',
    dni: '',
    email: '',
    alias: '',
    cvu: '',
    username: '',
    balance: 0,
    idAccount: '',
  };

  editingUsername = false;
  editingAlias = false;
  savingUsername = false;
  savingAlias = false;
  newUsername = '';
  newAlias = '';

  @Output() closed = new EventEmitter<void>();
  @Output() backdropClick = new EventEmitter<MouseEvent>();

  private sub?: Subscription;

  constructor(
    private userDataStore: UserDataStore,
    private toast: ToastService,
    private modalService: ModalService
  ) {}

  ngOnInit(): void {
    this.sub = this.userDataStore.userData$.subscribe((data) => {
      if (data) {
        this.userData = data;
      }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  get fullName(): string {
    return `${this.userData.name || ''} ${this.userData.lastName || ''}`.trim();
  }

  get initials(): string {
    const a = (this.userData.name || '?').charAt(0);
    const b = (this.userData.lastName || '').charAt(0);
    return `${a}${b}`.toUpperCase();
  }

  formatDni(dni: string | number | null | undefined): string {
    return formatDniAr(dni);
  }

  startEditUsername(): void {
    if (this.savingUsername || this.editingAlias) {
      return;
    }
    this.editingUsername = true;
    this.newUsername = this.userDataStore.getCurrent()?.username || '';
  }

  cancelEditUsername(): void {
    if (this.savingUsername) {
      return;
    }
    this.editingUsername = false;
    this.newUsername = '';
  }

  async saveUsername(): Promise<void> {
    const regex = /^(?=.*[A-Za-z])[A-Za-z\d]{4,25}$/;

    if (!regex.test(this.newUsername) || /^\d+$/.test(this.newUsername)) {
      this.toast.show(
        'Formato inválido. Solo letras y números, al menos una letra',
        'error'
      );
      return;
    }

    this.savingUsername = true;
    try {
      await this.userDataStore.updateUsername(this.newUsername);
      this.toast.show('Nombre de usuario actualizado correctamente', 'success');
      this.editingUsername = false;
    } catch (error) {
      logger.error('Error updating username:', error);
      this.toast.show('Error al actualizar el nombre de usuario', 'error');
    } finally {
      this.savingUsername = false;
    }
  }

  startEditAlias(): void {
    if (this.savingAlias || this.editingUsername) {
      return;
    }
    this.editingAlias = true;
    this.newAlias = this.userDataStore.getCurrent()?.alias || '';
  }

  cancelEditAlias(): void {
    if (this.savingAlias) {
      return;
    }
    this.editingAlias = false;
    this.newAlias = '';
  }

  async saveAlias(): Promise<void> {
    const aliasRegex =
      /^(?=.*[A-Za-z])(?=^[A-Za-z0-9]+(\.[A-Za-z0-9]+)+$)(?!.*\.\.)[A-Za-z0-9.]{4,25}$/;

    if (!aliasRegex.test(this.newAlias)) {
      this.toast.show('Formato de alias inválido', 'error');
      return;
    }

    this.savingAlias = true;
    try {
      await this.userDataStore.updateAlias(this.newAlias);
      this.toast.show('Alias actualizado correctamente', 'success');
      this.editingAlias = false;
    } catch (error) {
      logger.error('Error updating alias:', error);
      this.toast.show('Error al actualizar el alias', 'error');
    } finally {
      this.savingAlias = false;
    }
  }

  close(): void {
    if (this.savingUsername || this.savingAlias) {
      return;
    }
    this.editingAlias = false;
    this.editingUsername = false;
    this.modalService.closeModal();
    this.closed.emit();
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('profile-modal')) {
      this.close();
    }
  }
}
