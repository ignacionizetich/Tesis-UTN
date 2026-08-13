import { Component, OnInit, OnDestroy, EventEmitter, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth/auth.service';
import { ResendService } from '../../../services/resend/resend.service';
import { ToastService } from '../../../services/toast/toast.service';
import { maskEmail } from '../../../shared/utils/email-mask';
import { logger } from '../../../shared/utils/logger';

@Component({
  selector: 'app-forgot-password-form',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './forgot-password-form.html',
  styleUrls: ['./forgot-password-form.css'],
})
export class ForgotPasswordFormComponent implements OnInit, OnDestroy {
  @Output() emailSent = new EventEmitter<string>();

  email = '';
  isLoading = false;
  emailError = '';
  emailSentSuccess = false;
  showResendSection = false;
  resendCooldown = 0;
  resendTimer: ReturnType<typeof setInterval> | null = null;
  isResending = false;

  constructor(
    private authService: AuthService,
    private resendService: ResendService,
    private toast: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {}

  ngOnDestroy(): void {
    if (this.resendTimer) {
      clearInterval(this.resendTimer);
    }
  }

  onSubmit(): void {
    this.emailError = '';

    if (!this.email) {
      this.emailError = 'Por favor ingresá tu correo electrónico.';
      this.showToast('Campo requerido: Debes ingresar tu correo electrónico.', 'warning');
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.email)) {
      this.emailError = 'Por favor ingresá un correo electrónico válido.';
      this.showToast(
        'Formato incorrecto: Revisa que el correo tenga el formato correcto (ejemplo@correo.com).',
        'warning'
      );
      return;
    }

    this.isLoading = true;

    this.authService.sendRecoverMail(this.email).subscribe({
      next: () => {
        const censurado = maskEmail(this.email);
        this.showToast(
          `Correo enviado exitosamente a ${censurado}. Revisa tu bandeja de entrada.`,
          'info'
        );
        this.isLoading = false;
        this.emailSentSuccess = true;
        this.emailSent.emit(this.email);

        setTimeout(() => {
          this.showResendSection = true;
        }, 4000);
      },
      error: (error: { status?: number; error?: { message?: string } }) => {
        logger.error('Error en recuperación:', error);

        if (error.status === 401) {
          this.showToast(
            'Error de autenticación: Problema con el servidor. Intenta nuevamente.',
            'warning'
          );
        } else if (error.status === 404) {
          const mensaje =
            error.error?.message || 'El correo ingresado no está registrado en el sistema.';
          this.showToast(`${mensaje}`, 'warning');
        } else if (error.status === 429) {
          this.showToast(
            'Demasiados intentos: Espera unos minutos antes de intentar nuevamente.',
            'warning'
          );
        } else if (error.status && error.status >= 500) {
          this.showToast('Error del servidor: Intenta nuevamente en unos momentos.', 'error');
        } else if (error.status === 0 || !navigator.onLine) {
          this.showToast(
            'Sin conexión: Verifica tu conexión a internet e intenta nuevamente.',
            'warning'
          );
        } else {
          this.showToast(
            'Error inesperado: No se pudo procesar la solicitud. Intenta nuevamente.',
            'error'
          );
        }

        this.isLoading = false;
      },
    });
  }

  censurarCorreo(email: string): string {
    return maskEmail(email);
  }

  private showToast(message: string, type: 'success' | 'error' | 'info' | 'warning'): void {
    this.toast.show(message, type);
  }

  resendRecoveryEmail(): void {
    if (this.resendCooldown > 0 || this.isResending) {
      return;
    }

    this.isResending = true;

    this.resendService.resendPasswordRecovery(this.email).subscribe({
      next: () => {
        const censurado = maskEmail(this.email);
        this.showToast(`Correo reenviado exitosamente a ${censurado}.`, 'success');
        this.isResending = false;
        this.startResendCooldown();
      },
      error: (error: { status?: number }) => {
        logger.error('Error al reenviar:', error);

        if (error.status === 429) {
          this.showToast(
            'Demasiados intentos: Espera un momento antes de solicitar otro reenvío.',
            'warning'
          );
        } else {
          this.showToast('Error al reenviar: Intenta nuevamente en unos momentos.', 'error');
        }

        this.isResending = false;
      },
    });
  }

  private startResendCooldown(): void {
    this.resendCooldown = 60;

    this.resendTimer = setInterval(() => {
      this.resendCooldown--;

      if (this.resendCooldown <= 0 && this.resendTimer) {
        clearInterval(this.resendTimer);
        this.resendTimer = null;
      }
    }, 1000);
  }

  sendToAnotherEmail(): void {
    this.email = '';
    this.emailSentSuccess = false;
    this.showResendSection = false;
    this.resendCooldown = 0;

    if (this.resendTimer) {
      clearInterval(this.resendTimer);
      this.resendTimer = null;
    }
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
