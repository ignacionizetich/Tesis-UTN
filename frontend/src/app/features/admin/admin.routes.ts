import { Routes } from '@angular/router';
import { authGuard } from '../../guards/auth.guard';
import { adminGuard } from '../../guards/admin.guard';

export const ADMIN_ROUTES: Routes = [
  {
    path: 'admin',
    loadComponent: () =>
      import('../../pages/admin/admin').then((m) => m.AdminComponent),
    canActivate: [authGuard, adminGuard],
  },
];
