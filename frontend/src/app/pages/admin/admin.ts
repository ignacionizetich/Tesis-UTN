import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BackButtonComponent } from '../../components/ui/back-button/back-button';
import { ThemeToggleComponent } from '../../components/ui/theme-toggle/theme-toggle';
import { BrandLogoComponent } from '../../components/ui/brand-logo/brand-logo';
import { CreateAdminFormComponent } from './components/create-admin-form/create-admin-form';
import { UsersListComponent } from './components/users-list/users-list';

/**
 * Shell del panel admin: navegación entre vistas.
 * Formulario y listado viven en componentes hijos.
 */
@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    BackButtonComponent,
    ThemeToggleComponent,
    BrandLogoComponent,
    CreateAdminFormComponent,
    UsersListComponent,
  ],
  templateUrl: './admin.html',
  styleUrls: ['./admin.css'],
})
export class AdminComponent {
  currentView: 'main' | 'create-admin' | 'users-list' = 'main';

  showCreateAdminView(): void {
    this.currentView = 'create-admin';
  }

  showUsersListView(): void {
    this.currentView = 'users-list';
  }

  showMainView(): void {
    this.currentView = 'main';
  }
}
