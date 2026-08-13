import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ThemeService } from '../../services/theme/theme.service';
import { CreateAdminFormComponent } from './components/create-admin-form/create-admin-form';
import { UsersListComponent } from './components/users-list/users-list';
import { MetricsPanelComponent } from './components/metrics-panel/metrics-panel';
import { LoanRatesPanelComponent } from './components/loan-rates-panel/loan-rates-panel';

type AdminView = 'main' | 'users-list' | 'create-admin' | 'metrics' | 'loan-rates';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    CreateAdminFormComponent,
    UsersListComponent,
    MetricsPanelComponent,
    LoanRatesPanelComponent,
  ],
  templateUrl: './admin.html',
  styleUrls: ['./admin.css'],
})
export class AdminComponent {
  currentView: AdminView = 'main';

  constructor(
    private router: Router,
    private themeService: ThemeService
  ) {}

  get sectionTitle(): string {
    switch (this.currentView) {
      case 'users-list':
        return 'Usuarios autenticados';
      case 'create-admin':
        return 'Crear administrador';
      case 'metrics':
        return 'Métricas';
      case 'loan-rates':
        return 'Tasas de préstamos';
      default:
        return 'Panel de administración';
    }
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  goBack(): void {
    if (this.currentView === 'main') {
      this.goToDashboard();
      return;
    }
    this.currentView = 'main';
  }

  openView(view: AdminView): void {
    this.currentView = view;
  }

  showMainView(): void {
    this.currentView = 'main';
  }

  showUsersListView(): void {
    this.currentView = 'users-list';
  }

  showCreateAdminView(): void {
    this.currentView = 'create-admin';
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }
}
