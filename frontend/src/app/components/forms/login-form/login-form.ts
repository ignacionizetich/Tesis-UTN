import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth-service/auth.service';
import { ToastService } from '../../../services/toast-service/toast.service';
import { CacheService } from '../../../services/cache-service/cache.service';
import { ResendNavigationService } from '../../../services/resend-navigation/resend-navigation.service';

@Component({
  selector: 'app-login-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login-form.html',
  styleUrls: ['./login-form.css']
})
export class LoginFormComponent implements OnInit {
  @Output() loginSuccess = new EventEmitter<any>();

  loginForm: FormGroup;
  isLoading = false;
  showPassword = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private toast: ToastService,
    private cacheService: CacheService,
    private resendNavigationService: ResendNavigationService
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }

  ngOnInit() {
    this.clearAllCaches();

    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/dashboard'], { replaceUrl: true });
      return;
    }
  }

  private clearAllCaches(): void {
    try {
      this.cacheService.clearCachesByPrefix('arcash_');
    } catch (error) {
      console.error('Error limpiando cachés residuales:', error);
    }
  }

  onSubmit() {
    if (this.loginForm.valid && !this.isLoading) {
      this.isLoading = true;

      const loginData = {
        username: this.loginForm.get('username')?.value.trim(),
        password: this.loginForm.get('password')?.value.trim()
      };

      this.authService.loginAndPersist(loginData).subscribe({
        next: (response) => {
          this.isLoading = false;
          this.toast.show('Inicio de sesión exitoso.', 'success');
          this.loginForm.reset();

          this.loginSuccess.emit(response);

          setTimeout(() => {
            this.router.navigate(['/dashboard'], { replaceUrl: true });
          }, 2500);
        },
        error: (error) => {
          this.isLoading = false;
          console.error('Error en login:', error);

          if (error.status === 401) {
            this.toast.show('Nombre de usuario y/o contraseña incorrecta', 'error');
          } else if (error.status === 403) {
            this.toast.show('Cuenta inhabilitada, por favor confirma su cuenta', 'error');
          } else if (error.status >= 500) {
            this.toast.show('Error del servidor: Intenta nuevamente en unos momentos.', 'error');
          } else if (error.status === 0 || !navigator.onLine) {
            this.toast.show('Sin conexión: Verifica tu conexión a internet.', 'warning');
          } else {
            this.toast.show('Error inesperado: No se pudo iniciar sesión. Intenta nuevamente.', 'error');
          }
        }
      });
    } else {
      Object.keys(this.loginForm.controls).forEach(key => {
        this.loginForm.get(key)?.markAsTouched();
      });

      this.toast.show('Campos incompletos: Completa todos los campos requeridos.', 'warning');
    }
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  goTo(path: string) {
    this.router.navigate([`/${path}`]);
  }

  goToResend() {
    this.resendNavigationService.navigateFromLogin();
  }
}
