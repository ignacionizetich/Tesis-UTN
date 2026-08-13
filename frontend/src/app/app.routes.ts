import { Routes } from '@angular/router';
import { MARKETING_ROUTES } from './features/marketing/marketing.routes';
import { AUTH_ROUTES } from './features/auth/auth.routes';
import { WALLET_ROUTES } from './features/wallet/wallet.routes';
import { ADMIN_ROUTES } from './features/admin/admin.routes';

/**
 * Rutas por feature con lazy loadComponent.
 * Las pages siguen en pages/; features agrupa el mapa de navegación.
 */
export const routes: Routes = [
  ...MARKETING_ROUTES,
  ...AUTH_ROUTES,
  ...WALLET_ROUTES,
  ...ADMIN_ROUTES,
  { path: '**', redirectTo: '/404' },
];
