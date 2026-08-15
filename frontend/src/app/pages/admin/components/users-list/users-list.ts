import {
  Component,
  EventEmitter,
  OnInit,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../../services/admin/admin.service';
import { ToastService } from '../../../../services/toast/toast.service';
import { SessionStore } from '../../../../core/session/session.store';
import { UserResponse } from '../../../../models/admin.interface';
import { AuthenticatedInfoComponent } from '../../../../components/ui/authenticated-info/authenticated-info';
import { UserStatusConfirmModalComponent } from '../user-status-confirm-modal/user-status-confirm-modal';
import { logger } from '../../../../shared/utils/logger';
import { formatDni as formatDniAr } from '../../../../shared/utils/dni-format';

type RoleFilter = 'ALL' | 'USER' | 'ADMIN';

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
  isRoot = false;
  roleFilter: RoleFilter = 'ALL';

  private currentUserId = 0;

  /** ADMIN agrupa ADMIN + ROOT: para el filtro, "administradores" es todo lo que no es USER. */
  get filteredUsers(): UserResponse[] {
    switch (this.roleFilter) {
      case 'USER':
        return this.users.filter((u) => u.permissions === 'USER');
      case 'ADMIN':
        return this.users.filter((u) => u.permissions === 'ADMIN' || u.permissions === 'ROOT');
      default:
        return this.users;
    }
  }

  formatDni(dni: string | number | null | undefined): string {
    return formatDniAr(dni);
  }

  initials(user: UserResponse): string {
    const a = (user.name || '').trim().charAt(0);
    const b = (user.lastName || '').trim().charAt(0);
    return `${a}${b}`.toUpperCase() || '?';
  }

  /** Una fila de admin/root solo puede tocarse desde acá si quien mira es ROOT. */
  canToggle(user: UserResponse): boolean {
    return user.permissions === 'USER' || this.isRoot;
  }

  setRoleFilter(filter: RoleFilter): void {
    this.roleFilter = filter;
  }

  constructor(
    private adminService: AdminService,
    private toast: ToastService,
    private sessionStore: SessionStore
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.sessionStore.getCurrentUserIdHint();
    this.isRoot = this.sessionStore.getRole() === 'ROOT';
    if (this.currentUserId <= 0) {
      logger.warn('No se pudo obtener el ID del usuario actual desde SessionStore');
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
        logger.error('Error al cargar usuarios:', error);
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
    const isAdminTarget = user.permissions === 'ADMIN';
    this.loadingUserAction = user.id;

    const serviceCall = isAdminTarget
      ? (user.active ? this.adminService.disableAdmin(user.id) : this.adminService.enableAdmin(user.id))
      : (user.active ? this.adminService.disableUser(user.id) : this.adminService.enableUser(user.id));

    serviceCall.subscribe({
      next: () => {
        user.active = !user.active;
        this.adminService.updateUserInCache(user.id, user.active);
        this.toast.show(
          `${isAdminTarget ? 'Administrador' : 'Usuario'} ${action === 'deshabilitar' ? 'deshabilitado' : 'habilitado'} exitosamente`,
          'success'
        );
        this.usersAlreadyLoaded = false;
        this.loadingUserAction = null;
        this.closeConfirmModal();
        this.resetUserModal();
      },
      error: (error) => {
        logger.error(`Error al ${action} ${isAdminTarget ? 'administrador' : 'usuario'}:`, error);
        this.toast.show(`Error al ${action} ${isAdminTarget ? 'administrador' : 'usuario'}`, 'error');
        this.loadingUserAction = null;
        this.closeConfirmModal();
      },
    });
  }

  // NOTA: currentUserId viene de SessionStore.getCurrentUserIdHint(), que hoy
  // devuelve el ID de CUENTA (idAccount), no el ID de usuario — porque
  // UserData/LoginResponse nunca cargan el ID de usuario real. Comparar
  // user.id (ID de usuario) contra esto compara dos cosas distintas y puede
  // dar falsos positivos por coincidencia numérica (pasó con id=4).
  private isCurrentUser(user: UserResponse): boolean {
    return user.idAccount === this.currentUserId;
  }
}
