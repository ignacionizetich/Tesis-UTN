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

@Component({
  selector: 'app-profile-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile-modal.html',
  styleUrls: ['../../styles/modals-shared.css'],
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

  startEditUsername(): void {
    this.editingUsername = true;
    this.newUsername = this.userDataStore.getCurrent()?.username || '';
  }

  cancelEditUsername(): void {
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

    try {
      await this.userDataStore.updateUsername(this.newUsername);
      this.toast.show(
        'Nombre de usuario actualizado correctamente',
        'success'
      );
      this.editingUsername = false;
    } catch (error) {
      console.error('Error updating username:', error);
      this.toast.show('Error al actualizar el nombre de usuario', 'error');
    }
  }

  startEditAlias(): void {
    this.editingAlias = true;
    this.newAlias = this.userDataStore.getCurrent()?.alias || '';
  }

  cancelEditAlias(): void {
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

    try {
      await this.userDataStore.updateAlias(this.newAlias);
      this.toast.show('Alias actualizado correctamente', 'success');
      this.editingAlias = false;
    } catch (error) {
      console.error('Error updating alias:', error);
      this.toast.show('Error al actualizar el alias', 'error');
    }
  }

  close(): void {
    this.editingAlias = false;
    this.editingUsername = false;
    this.modalService.closeModal();
    this.closed.emit();
  }

  onBackdrop(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal')) {
      this.close();
    }
  }
}
