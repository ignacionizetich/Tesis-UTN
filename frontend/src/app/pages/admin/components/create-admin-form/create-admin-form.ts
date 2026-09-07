import {
  Component,
  EventEmitter,
  OnInit,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { AdminService } from '../../../../services/admin/admin.service';
import { ToastService } from '../../../../services/toast/toast.service';
import { AdminRequest } from '../../../../models/admin.interface';
import {
  AUTH_RULES,
  emailMatchValidator,
  passwordMatchValidator,
  passwordNotSimilarToIdentityValidator,
  strongPasswordValidator,
} from '../../../../shared/validators/auth.validators';
import { maskEmail } from '../../../../shared/utils/email-mask';
import { logger } from '../../../../shared/utils/logger';
import { apiError, fieldErrorsOf } from '../../../../shared/utils/error-message';

@Component({
  selector: 'app-create-admin-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './create-admin-form.html',
  styleUrls: ['../../admin.css'],
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
          Validators.pattern(AUTH_RULES.personName),
          Validators.minLength(AUTH_RULES.personNameMinLength),
          Validators.maxLength(AUTH_RULES.personNameMaxLength),
        ],
      ],
      lastName: [
        '',
        [
          Validators.required,
          Validators.pattern(AUTH_RULES.personName),
          Validators.minLength(AUTH_RULES.personNameMinLength),
          Validators.maxLength(AUTH_RULES.personNameMaxLength),
        ],
      ],
      dni: ['', [Validators.required, Validators.pattern(AUTH_RULES.dni)]],
      username: [
        '',
        [
          Validators.required,
          Validators.minLength(AUTH_RULES.usernameMinLength),
          Validators.maxLength(AUTH_RULES.usernameMaxLength),
          Validators.pattern(AUTH_RULES.username),
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
    }, {
      // El backend aplica la misma restricción al alta de administradores.
      validators: passwordNotSimilarToIdentityValidator([
        'username',
        'emails.email',
        'dni',
        'name',
        'lastName',
      ]),
    });
  }

  /** El campo de identidad que la contraseña está repitiendo, si hay alguno. */
  get passwordIdentityConflict(): string | null {
    const conflict = this.form?.errors?.['passwordLikeIdentity'];
    if (!conflict) {
      return null;
    }
    const labels: Record<string, string> = {
      username: 'nombre de usuario',
      'emails.email': 'email',
      dni: 'DNI',
      name: 'nombre',
      lastName: 'apellido',
    };
    return labels[conflict.field] ?? 'datos personales';
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
        logger.error('Error en creación de admin:', error);
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
    // El backend indica el campo en conflicto en `fieldErrors`, así que se puede armar un
    // mensaje concreto sin comparar el texto en castellano, que puede reescribirse.
    const conflicto = fieldErrorsOf(error)[0]?.field;

    if (conflicto === 'email') {
      this.toast.show(
        `El correo ${maskEmail(adminRequest.email)} ya está registrado en el sistema. Por favor, utiliza otro correo electrónico.`,
        'warning'
      );
      return;
    }
    if (conflicto === 'username') {
      this.toast.show(
        `El nombre de usuario "${adminRequest.username}" no está disponible. Por favor, elige otro nombre de usuario.`,
        'warning'
      );
      return;
    }
    if (conflicto === 'dni') {
      this.toast.show(
        `El DNI ${adminRequest.dni} ya está registrado en el sistema. Verifica los datos e intenta nuevamente.`,
        'warning'
      );
      return;
    }

    // Validación de campos: el backend detalla cuáles fallaron.
    const invalidos = fieldErrorsOf(error);
    if (invalidos.length > 0) {
      this.toast.show(
        `Revisá estos campos: ${invalidos.map((f) => f.field).join(', ')}.`,
        'warning'
      );
      return;
    }

    const backendMessage = apiError(error)?.message;
    if (backendMessage) {
      this.toast.show(`Error al crear administrador: ${backendMessage}`, 'error');
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
