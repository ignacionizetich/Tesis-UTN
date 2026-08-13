import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ThemeToggleComponent } from '../../components/ui/theme-toggle/theme-toggle';
import { BrandLogoComponent } from '../../components/ui/brand-logo/brand-logo';
import { Footer } from '../../components/footer/footer';
import { ToastService } from '../../services/toast/toast.service';
import {
  ValidationService,
  type ValidationResponse,
} from '../../services/validation/validation.service';
import { ResendService } from '../../services/resend/resend.service';
import { maskEmail } from '../../shared/utils/email-mask';
import { logger } from '../../shared/utils/logger';

@Component({
  selector: 'app-validate',
  standalone: true,
  imports: [CommonModule, FormsModule, ThemeToggleComponent, BrandLogoComponent, Footer],
  templateUrl: './validate.html',
  styleUrls: ['./validate.css'],
})
export class ValidateComponent implements OnInit, OnDestroy {
  validationResult: ValidationResponse | null = null;
  isLoading = true;
  isResending = false;
  showResendForm = false;
  resendEmail = '';

  private loadSub?: { unsubscribe(): void };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private toast: ToastService,
    private validationService: ValidationService,
    private resendService: ResendService
  ) {}

  ngOnInit(): void {
    this.loadSub = this.route.queryParams.subscribe((params) => {
      const token = params['token'];
      this.validateToken(token);
    });
  }

  private validateToken(token: string): void {
    this.validationService.validateEmailToken(token).subscribe({
      next: (response: ValidationResponse) => {
        this.validationResult = response;
        this.isLoading = false;

        if (response.success) {
          this.toast.show('¡Cuenta verificada! Ya podés iniciar sesión.', 'success');
        } else if (response.message.includes('ya fue utilizado')) {
          this.toast.show('Esta cuenta ya está activada.', 'info');
        } else if (response.message.includes('expirado')) {
          this.toast.show('El enlace expiró. Pedí uno nuevo.', 'warning');
        } else {
          this.toast.show(response.message, 'error');
        }
      },
      error: (error: { status?: number }) => {
        logger.error('Error validating token:', error);

        if (error.status === 404 || error.status === 400) {
          this.router.navigate(['/404']);
          return;
        }

        this.validationResult = {
          success: false,
          message: 'No pudimos validar el enlace. Intentá de nuevo.',
        };
        this.toast.show('Error de conexión', 'error');
        this.isLoading = false;
      },
    });
  }

  get title(): string {
    if (this.isLoading) {
      return 'Validando tu cuenta';
    }
    if (this.validationResult?.success) {
      return 'Cuenta activada';
    }
    return 'No se pudo validar';
  }

  get message(): string {
    if (this.isLoading) {
      return 'Estamos confirmando tu email. Un momento…';
    }
    return this.validationResult?.message || 'No encontramos un token válido en el enlace.';
  }

  get isSuccess(): boolean {
    return !!this.validationResult?.success;
  }

  showResendOption(): boolean {
    return this.validationResult?.success === false && !this.isLoading;
  }

  openResendForm(): void {
    this.showResendForm = true;
  }

  resendValidationEmail(): void {
    if (this.isResending) {
      return;
    }

    const email = this.resendEmail.trim();
    if (!email) {
      this.toast.show('Ingresá tu correo para reenviar el enlace.', 'warning');
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      this.toast.show('Ingresá un correo válido.', 'warning');
      return;
    }

    this.isResending = true;

    this.resendService.resendValidationEmail(email).subscribe({
      next: () => {
        this.toast.show(`Correo reenviado a ${maskEmail(email)}.`, 'success');
        this.isResending = false;
        this.showResendForm = false;
        this.resendEmail = '';
      },
      error: (error: { status?: number }) => {
        logger.error('Error al reenviar:', error);
        if (error.status === 429) {
          this.toast.show('Demasiados intentos. Esperá un momento.', 'warning');
        } else {
          this.toast.show('No se pudo reenviar. Intentá más tarde.', 'error');
        }
        this.isResending = false;
      },
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  goToHome(): void {
    this.router.navigate(['/']);
  }

  ngOnDestroy(): void {
    this.loadSub?.unsubscribe();
  }
}
