import { Routes } from '@angular/router';
import { guestGuard } from '../../guards/guest.guard';
import { validateGuard } from '../../guards/validate.guard';
import { resendGuard } from '../../guards/resend.guard';

export const AUTH_ROUTES: Routes = [
  {
    path: 'register',
    loadComponent: () =>
      import('../../pages/register/register').then((m) => m.RegisterComponent),
    canActivate: [guestGuard],
  },
  {
    path: 'login',
    loadComponent: () =>
      import('../../pages/login/login').then((m) => m.LoginComponent),
    canActivate: [guestGuard],
  },
  {
    path: 'forgot',
    loadComponent: () =>
      import('../../pages/forgot/forgot').then((m) => m.ForgotComponent),
  },
  {
    path: 'resend',
    loadComponent: () =>
      import('../../pages/resend/resend').then((m) => m.ResendComponent),
    canActivate: [resendGuard],
  },
  {
    path: 'validate',
    loadComponent: () =>
      import('../../pages/validate/validate').then((m) => m.ValidateComponent),
    canActivate: [validateGuard],
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('../../pages/recover-password/recover-password').then(
        (m) => m.RecoverPasswordComponent,
      ),
  },
];
