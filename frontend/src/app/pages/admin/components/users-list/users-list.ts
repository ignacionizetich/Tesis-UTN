import {
  Component,
  EventEmitter,
  OnInit,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../../services/admin-service/admin.service';
import { ToastService } from '../../../../services/toast-service/toast.service';
import { SessionStore } from '../../../../core/session/session.store';
import { UserResponse } from '../../../../models/admin.interface';
import { AuthenticatedInfoComponent } from '../../../../components/ui/authenticated-info/authenticated-info';
import { UserStatusConfirmModalComponent } from '../user-status-confirm-modal/user-status-confirm-modal';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [CommonModule, AuthenticatedInfoComponent, UserStatusConfirmModalComponent],
  templateUrl: './users-list.html',
  styleUrls: ['../../admin.css', '../../styles/users-list.css'],
})
export class UsersListComponent implements OnInit {
  @Output() goToCreate = new EventEmitter<void>();

  users: UserResponse[] = [];
  isLoadingUsers = false;
  usersAlreadyLoaded = false;
  selectedUser: UserResponse | null = null;
  showUserModal = false;
  showConfirmModal = false;
  userToToggle: UserResponse | null = null;
  loadingUserAction: number | null = null;

  private currentUserId = 0;

  constructor(
    private adminService: AdminService,
    private toast: ToastService,
    private sessionStore: SessionStore
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.sessionStore.getCurrentUserIdHint();
    if (this.currentUserId <= 0) {
      console.warn('No se pudo obtener el ID del usuario actual desde SessionStore');
    }
    this.loadUsers();
  }

  refreshUsersList(): void {
    this.usersAlreadyLoaded = false;
    this.loadUsers(true);
  }

  loadUsers(forceReload: boolean = false): void {
    if (this.usersAlreadyLoaded && !forceReload) {
      this.isLoadingUsers = true;
      setTimeout(() => {
        this.isLoadingUsers = false;
      }, 150);
      return;
    }

    this.isLoadingUsers = true;

    if (this.currentUserId === 0) {
      this.currentUserId = this.sessionStore.getCurrentUserIdHint();
    }

    this.adminService.getAuthenticatedUsers().subscribe({
      next: (users) => {
        this.users = users.filter((user) => !this.isCurrentUser(user));
        this.adminService.cacheUsers(this.users);
        this.usersAlreadyLoaded = true;
        this.isLoadingUsers = false;
      },
      error: (error) => {
        console.error('Error al cargar usuarios:', error);
        this.toast.show('Error al cargar usuarios', 'error');
        this.isLoadingUsers = false;

        const cachedUsers = this.adminService.getCachedUsers();
        if (cachedUsers) {
          this.users = cachedUsers.filter((user) => !this.isCurrentUser(user));
        }
      },
    });
  }

  openUserModal(user: UserResponse): void {
    requestAnimationFrame(() => {
      this.selectedUser = user;
      this.showUserModal = true;
    });
  }

  resetUserModal(): void {
    this.showUserModal = false;
    this.selectedUser = null;
  }

  onToggleUserStatus(user: UserResponse): void {
    this.resetUserModal();
    this.openConfirmModal(user);
  }

  openConfirmModal(user: UserResponse): void {
    this.userToToggle = user;
    this.showConfirmModal = true;
  }

  closeConfirmModal(): void {
    this.userToToggle = null;
    this.showConfirmModal = false;
    this.loadingUserAction = null;
  }

  confirmToggleUser(): void {
    if (!this.userToToggle) {
      return;
    }

    const user = this.userToToggle;
    const action = user.active ? 'deshabilitar' : 'habilitar';
    this.loadingUserAction = user.id;

    const serviceCall = user.active
      ? this.adminService.disableUser(user.id)
      : this.adminService.enableUser(user.id);

    serviceCall.subscribe({
      next: () => {
        user.active = !user.active;
        this.adminService.updateUserInCache(user.id, user.active);
        this.toast.show(
          `Usuario ${action === 'deshabilitar' ? 'deshabilitado' : 'habilitado'} exitosamente`,
          'success'
        );
        this.usersAlreadyLoaded = false;
        this.loadingUserAction = null;
        this.closeConfirmModal();
        this.resetUserModal();
      },
      error: (error) => {
        console.error(`Error al ${action} usuario:`, error);
        this.toast.show(`Error al ${action} usuario`, 'error');
        this.loadingUserAction = null;
        this.closeConfirmModal();
      },
    });
  }

  private isCurrentUser(user: UserResponse): boolean {
    return (
      user.id === this.currentUserId ||
      user.idAccount === this.currentUserId ||
      Number(user.id) === this.currentUserId
    );
  }
}
