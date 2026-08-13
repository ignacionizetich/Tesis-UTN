import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ThemeToggleComponent } from '../../components/ui/theme-toggle/theme-toggle';
import { BackButtonComponent } from '../../components/ui/back-button/back-button';
import { BrandLogoComponent } from '../../components/ui/brand-logo/brand-logo';
import { ForgotPasswordFormComponent } from '../../components/forms/forgot-password-form/forgot-password-form';
import { Footer } from '../../components/footer/footer';
import { logger } from '../../shared/utils/logger';

@Component({
  selector: 'app-forgot',
  standalone: true,
  imports: [
    CommonModule,
    ThemeToggleComponent,
    BackButtonComponent,
    BrandLogoComponent,
    ForgotPasswordFormComponent,
    Footer,
  ],
  templateUrl: './forgot.html',
  styleUrls: ['./forgot.css'],
})
export class ForgotComponent {
  onEmailSent(): void {
    logger.debug('Correo de recuperación enviado correctamente');
  }
}
