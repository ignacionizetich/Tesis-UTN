import { Component, OnInit, OnDestroy, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth/auth.service';
import { ResendService } from '../../../services/resend/resend.service';
import { ToastService } from '../../../services/toast/toast.service';
import {
  AUTH_RULES,
  passwordMatchValidator,
  emailMatchValidator,
  passwordNotSimilarToIdentityValidator,
  strongPasswordValidator,
} from '../../../shared/validators/auth.validators';
import { maskEmail } from '../../../shared/utils/email-mask';
import { logger } from '../../../shared/utils/logger';
import { errorMessage, fieldErrorsOf } from '../../../shared/utils/error-message';

@Component({
  selector: 'app-register-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './register-form.html',
  styleUrls: ['./register-form.css']
})
export class RegisterFormComponent implements OnInit, OnDestroy {
  @Output() registerSuccess = new EventEmitter<string>();

  registerForm!: FormGroup;
  showPassword = false;
  showConfirmPassword = false;
  loading = false;
  registrationSuccessful = false;
  registeredEmail = '';
  showResendSection = false;
  isResending = false;
  resendCooldown = 0;
  resendTimer: any;

  constructor(
    private fb: FormBuilder,
    private toast: ToastService, 
    private authService: AuthService, 
    private resendService: ResendService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.pattern(AUTH_RULES.personName), Validators.minLength(AUTH_RULES.personNameMinLength), Validators.maxLength(AUTH_RULES.personNameMaxLength)]],
      apellido: ['', [Validators.required, Validators.pattern(AUTH_RULES.personName), Validators.minLength(AUTH_RULES.personNameMinLength), Validators.maxLength(AUTH_RULES.personNameMaxLength)]],
      dni: ['', [Validators.required, Validators.pattern(AUTH_RULES.dni)]],
      alias: ['', [Validators.required, Validators.minLength(AUTH_RULES.usernameMinLength), Validators.maxLength(AUTH_RULES.usernameMaxLength), Validators.pattern(AUTH_RULES.username)]],

      emails: this.fb.group({
        email: ['', [Validators.required, Validators.email, Validators.pattern('^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$')]],
        confirmEmail: ['', [Validators.required, Validators.email]]
      }, { 
        validators: emailMatchValidator
      }),
      
      passwords: this.fb.group({
        password: ['', [Validators.required, strongPasswordValidator]],
        confirmPassword: ['', [Validators.required]]
      }, { 
        validators: passwordMatchValidator
      })
    }, {
      // A nivel del formulario porque compara la contraseña contra campos que están fuera de
      // su propio grupo. Es la misma regla que aplica el backend al recibir el registro.
      validators: passwordNotSimilarToIdentityValidator(['alias', 'emails.email', 'dni', 'nombre', 'apellido'])
    });
  }

  /** El campo de identidad que la contraseña está repitiendo, si hay alguno. */
  get passwordIdentityConflict(): string | null {
    const conflict = this.registerForm?.errors?.['passwordLikeIdentity'];
    if (!conflict) {
      return null;
    }
    const labels: Record<string, string> = {
      alias: 'nombre de usuario',
      'emails.email': 'email',
      dni: 'DNI',
      nombre: 'nombre',
      apellido: 'apellido',
    };
    return labels[conflict.field] ?? 'datos personales';
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      this.toast.show("Formulario incompleto: Revisa y completa todos los campos marcados en rojo.", "warning");
      return;
    }

    if (this.loading) return;

    this.loading = true;

    const formData = this.registerForm.value;
    const userData = {
      name: formData.nombre,
      lastName: formData.apellido,
      dni: formData.dni,
      email: formData.emails.email,
      password: formData.passwords.password,
      alias: formData.alias
    };

    this.authService.registerUser(userData).subscribe({
      next: (response) => {
        this.loading = false;
        this.registrationSuccessful = true;
        this.registeredEmail = userData.email;
        
        const emailCensurado = maskEmail(userData.email);
        this.toast.show(`¡Registro exitoso! Se envió un correo de validación a ${emailCensurado}. Revisa tu bandeja de entrada para activar tu cuenta.`, "success");
        
        this.registerSuccess.emit(userData.email);
        
        setTimeout(() => {
          this.showResendSection = true;
        }, 4000);
      },
      error: (error) => {
        this.loading = false;
        logger.error('Error en registro:', error);
        this.applyServerFieldErrors(error);

        const backendMessage = errorMessage(error, '');

        if (backendMessage) {
          const esDatoRechazado = error.status >= 400 && error.status < 500;
          this.toast.show(backendMessage, esDatoRechazado ? "warning" : "error");
        } else if (error.status === 0 || !navigator.onLine) {
          this.toast.show("Sin conexión. Verifica tu conexión a internet e intenta nuevamente.", "warning");
        } else if (error.status >= 500) {
          this.toast.show("Error del servidor. Intenta registrarte nuevamente en unos momentos.", "error");
        } else if (error.status === 400) {
          this.toast.show("Datos inválidos. Revisa que todos los campos tengan el formato correcto.", "warning");
        } else {
          this.toast.show("Error inesperado. No se pudo completar el registro. Intenta nuevamente.", "error");
        }
      }
    });
  }

  /**
   * Marca en el formulario los campos que el backend rechazó.
   *
   * El DTO del servidor usa `name`/`lastName`/`email`/`password`; acá viven con otros
   * nombres y a veces dentro de un grupo. Sin este mapeo el toast avisa pero el input
   * no se pinta en rojo.
   */
  private applyServerFieldErrors(error: unknown): void {
    const paths: Record<string, string> = {
      name: 'nombre',
      lastName: 'apellido',
      dni: 'dni',
      alias: 'alias',
      username: 'alias',
      email: 'emails.email',
      password: 'passwords.password',
    };

    for (const fieldError of fieldErrorsOf(error)) {
      const path = paths[fieldError.field] ?? fieldError.field;
      const control = this.registerForm.get(path);
      if (control) {
        control.setErrors({ ...(control.errors ?? {}), server: fieldError.message });
        control.markAsTouched();
      }
    }
  }

  onDniInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value;
    
    value = value.replace(/\D/g, '');
    
    if (value.length > 8) {
      value = value.substring(0, 8);
    }
    
    input.value = value;
    this.registerForm.get('dni')?.setValue(value);
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPasswordVisibility() {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  /** Usado por el template. */
  censurarCorreo(email: string): string {
    return maskEmail(email);
  }

  resendValidationEmail(): void {
    if (this.resendCooldown > 0 || this.isResending) return;

    this.isResending = true;
    
    this.resendService.resendValidationEmail(this.registeredEmail).subscribe({
      next: (response) => {
        const censurado = maskEmail(this.registeredEmail);
        this.toast.show(`Correo reenviado exitosamente a ${censurado}.`, 'success');
        this.isResending = false;
        this.startResendCooldown();
      },
      error: (error) => {
        logger.error('Error al reenviar:', error);
        
        if (error.status === 429) {
          this.toast.show('Demasiados intentos: Espera un momento antes de solicitar otro reenvío.', 'warning');
        } else {
          this.toast.show('Error al reenviar: Intenta nuevamente en unos momentos.', 'error');
        }
        
        this.isResending = false;
      }
    });
  }

  private startResendCooldown(): void {
    this.resendCooldown = 60;
    
    this.resendTimer = setInterval(() => {
      this.resendCooldown--;
      
      if (this.resendCooldown <= 0) {
        clearInterval(this.resendTimer);
        this.resendTimer = null;
      }
    }, 1000);
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  ngOnDestroy(): void {
    if (this.resendTimer) {
      clearInterval(this.resendTimer);
    }
  }
}
