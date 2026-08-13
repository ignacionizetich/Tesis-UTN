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
  passwordMatchValidator,
  emailMatchValidator,
  strongPasswordValidator,
} from '../../../shared/validators/auth.validators';
import { maskEmail } from '../../../shared/utils/email-mask';

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
      nombre: ['', [Validators.required, Validators.pattern('[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]{2,50}'), Validators.minLength(2), Validators.maxLength(50)]],
      apellido: ['', [Validators.required, Validators.pattern('[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]{2,50}'), Validators.minLength(2), Validators.maxLength(50)]],
      dni: ['', [Validators.required, Validators.pattern('^\\d{8}$'), Validators.minLength(8), Validators.maxLength(8)]],
      alias: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(10), Validators.pattern('^[a-zA-Z0-9_-]+$')]],
      
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
    });
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
        console.error('Error en registro:', error);
        
        const backendMessage = error.error?.message;
        
        if (backendMessage) {
          if (backendMessage.includes("email ya se encuentra en uso") ||
              backendMessage.includes("nombre de usuario no está disponible") ||
              backendMessage.includes("DNI ya está registrado")) {
            this.toast.show(backendMessage, "warning");
          } else if (backendMessage.includes("campos son obligatorios")) {
            this.toast.show("Todos los campos son obligatorios.", "warning");
          } else {
            this.toast.show(backendMessage, "error");
          }
        } else {
          if (error.status === 400) {
            this.toast.show("Datos inválidos. Revisa que todos los campos tengan el formato correcto.", "warning");
          } else if (error.status >= 500) {
            this.toast.show("Error del servidor. Intenta registrarte nuevamente en unos momentos.", "error");
          } else if (error.status === 0 || !navigator.onLine) {
            this.toast.show("Sin conexión. Verifica tu conexión a internet e intenta nuevamente.", "warning");
          } else {
            this.toast.show("Error inesperado. No se pudo completar el registro. Intenta nuevamente.", "error");
          }
        }
      }
    });
  }

  onDniInput(event: any): void {
    const input = event.target;
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
        console.error('Error al reenviar:', error);
        
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
