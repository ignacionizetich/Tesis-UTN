import { Routes } from '@angular/router';
import { homeGuard } from '../../guards/home.guard';

export const MARKETING_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('../../pages/home/home').then((m) => m.Home),
    canActivate: [homeGuard],
  },
  {
    path: '404',
    loadComponent: () =>
      import('../../pages/error-404/error-404').then((m) => m.Error404Component),
  },
];
