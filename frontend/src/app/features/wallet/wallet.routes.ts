import { Routes } from '@angular/router';
import { authGuard } from '../../guards/auth.guard';

export const WALLET_ROUTES: Routes = [
  {
    path: 'dashboard',
    loadComponent: () =>
      import('../../pages/dashboard/dashboard').then((m) => m.DashboardComponent),
    canActivate: [authGuard],
  },
  {
    path: 'usd-account',
    loadComponent: () =>
      import('../../pages/usd-account/usd-account').then((m) => m.UsdAccountComponent),
    canActivate: [authGuard],
  },
];
