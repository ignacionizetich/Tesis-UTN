import { AbstractControl, ValidationErrors } from '@angular/forms';

/** Comprueba que `password` y `confirmPassword` coincidan (grupo). */
export function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  return password === confirmPassword ? null : { passwordMismatch: true };
}

/** Comprueba que `email` y `confirmEmail` coincidan (grupo). */
export function emailMatchValidator(control: AbstractControl): ValidationErrors | null {
  const email = control.get('email')?.value;
  const confirmEmail = control.get('confirmEmail')?.value;
  return email === confirmEmail ? null : { emailMismatch: true };
}

/** Contraseña fuerte: min 8, mayúscula, minúscula, número y carácter especial. */
export function strongPasswordValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (!value) {
    return null;
  }

  const errors: ValidationErrors = {};

  if (value.length < 8) {
    errors['minLength'] = true;
  }
  if (!/[a-z]/.test(value)) {
    errors['lowercase'] = true;
  }
  if (!/[A-Z]/.test(value)) {
    errors['uppercase'] = true;
  }
  if (!/\d/.test(value)) {
    errors['number'] = true;
  }
  if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\?]/.test(value)) {
    errors['specialChar'] = true;
  }

  return Object.keys(errors).length > 0 ? errors : null;
}
