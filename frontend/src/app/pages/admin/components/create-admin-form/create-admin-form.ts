import {
  Component,
  EventEmitter,
  OnInit,
  Output,
  ViewEncapsulation,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { AdminService } from '../../../../services/admin-service/admin.service';
import { ToastService } from '../../../../services/toast-service/toast.service';
import { AdminRequest } from '../../../../models/admin.interface';
import {
  emailMatchValidator,
  passwordMatchValidator,
  strongPasswordValidator,
} from '../../../../shared/validators/auth.validators';
import { maskEmail } from '../../../../shared/utils/email-mask';

@Component({
  selector: 'app-create-admin-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './create-admin-form.html',
  styleUrls: ['../../admin.css'],
  encapsulation: ViewEncapsulation.None,
})
export class CreateAdminFormComponent implements OnInit {
  @Output() goToUsers = new EventEmitter<void>();

  form!: FormGroup;
  showPassword = false;
  showConfirmPassword = false;
  loading = false;

  constructor(
    private fb: FormBuilder,
    private adminService: AdminService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: [
        '',
        [
          Validators.required,
          Validators.pattern('[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]{2,50}'),
          Validators.minLength(2),
          Validators.maxLength(50),
        ],
      ],
      lastName: [
        '',
        [
          Validators.required,
          Validators.pattern('[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]{2,50}'),
          Validators.minLength(2),
          Validators.maxLength(50),
        ],
      ],
      dni: [
        '',
        [
          Validators.required,
          Validators.pattern('^\\d{8}$'),
          Validators.minLength(8),
          Validators.maxLength(8),
        ],
      ],
      username: [
        '',
        [
          Validators.required,
          Validators.minLength(3),
          Validators.maxLength(10),
          Validators.pattern('^[a-zA-Z0-9_-]+$'),
        ],
      ],
      emails: this.fb.group(
        {
          email: [
            '',
            [
              Validators.required,
              Validators.email,
              Validators.pattern(
                '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$'
              ),
            ],
          ],
          confirmEmail: ['', [Validators.required, Validators.email]],
        },
        { validators: emailMatchValidator }
      ),
      passwords: this.fb.group(
        {
          password: ['', [Validators.required, strongPasswordValidator]],
          confirmPassword: ['', [Validators.required]],
        },
        { validators: passwordMatchValidator }
      ),
    });
  }

  createAdmin(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.toast.show(
        'Formulario incompleto: Revisa y completa todos los campos marcados en rojo.',
        'warning'
      );
      return;
    }

    if (this.loading) {
      return;
    }

    this.loading = true;

    const formData = this.form.value;
    const adminRequest: AdminRequest = {
      name: formData.name,
      lastName: formData.lastName,
      dni: formData.dni,
      email: formData.emails.email,
      username: formData.username,
      password: formData.passwords.password,
    };

    this.adminService.createAdmin(adminRequest).subscribe({
      next: () => {
        this.loading = false;
        const emailCensurado = maskEmail(adminRequest.email);
        this.toast.show(
          `¡Administrador creado exitosamente! Se ha creado la cuenta para ${adminRequest.name} ${adminRequest.lastName} (${emailCensurado}) con permisos de administrador.`,
          'success'
        );
        this.form.reset();
      },
      error: (error) => {
        this.loading = false;
        console.error('Error en creación de admin:', error);
        this.handleCreateError(error, adminRequest);
      },
    });
  }

  onDniInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, '');
    if (value.length > 8) {
      value = value.substring(0, 8);
    }
    input.value = value;
    this.form.get('dni')?.setValue(value);
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  private handleCreateError(error: any, adminRequest: AdminRequest): void {
    const backendMessage = error.error?.mensaje || error.error?.message;

    if (backendMessage) {
      if (backendMessage.includes('email ya se encuentra en uso')) {
        const emailCensurado = maskEmail(adminRequest.email);
        this.toast.show(
          `El correo ${emailCensurado} ya está registrado en el sistema. Por favor, utiliza otro correo electrónico.`,
          'warning'
        );
      } else if (backendMessage.includes('nombre de usuario no está disponible')) {
        this.toast.show(
          `El nombre de usuario "${adminRequest.username}" no está disponible. Por favor, elige otro nombre de usuario.`,
          'warning'
        );
      } else if (backendMessage.includes('DNI ya está registrado')) {
        this.toast.show(
          `El DNI ${adminRequest.dni} ya está registrado en el sistema. Verifica los datos e intenta nuevamente.`,
          'warning'
        );
      } else if (backendMessage.includes('campos son obligatorios')) {
        this.toast.show(
          'Todos los campos son obligatorios para crear un administrador.',
          'warning'
        );
      } else {
        this.toast.show(
          `Error al crear administrador: ${backendMessage}`,
          'error'
        );
      }
      return;
    }

    if (error.status === 400) {
      this.toast.show(
        'Datos inválidos: Revisa que todos los campos tengan el formato correcto y que las contraseñas coincidan.',
        'warning'
      );
    } else if (error.status === 403) {
      this.toast.show(
        'Acceso denegado: No tienes permisos suficientes para crear administradores.',
        'error'
      );
    } else if (error.status === 409) {
      this.toast.show(
        'Conflicto: Los datos ingresados ya existen en el sistema.',
        'warning'
      );
    } else if (error.status >= 500) {
      this.toast.show(
        'Error del servidor: No se pudo crear el administrador en este momento. Intenta nuevamente en unos minutos.',
        'error'
      );
    } else if (error.status === 0 || !navigator.onLine) {
      this.toast.show(
        'Sin conexión: Verifica tu conexión a internet e intenta crear el administrador nuevamente.',
        'warning'
      );
    } else {
      this.toast.show(
        'Error inesperado: No se pudo crear el administrador. Verifica los datos e intenta nuevamente.',
        'error'
      );
    }
  }
}
