import { AbstractControl, ValidationErrors } from '@angular/forms';

/**
 * Reglas de formato compartidas con el backend.
 *
 * Cada constante replica una restricción de `com.EDJ.ArCash.validation`. Cuando el formulario
 * es más estricto que el servidor el usuario no puede enviar datos que en realidad son válidos
 * (por ejemplo un DNI de 7 dígitos), y cuando es más permisivo el servidor rechaza el envío
 * después de completar todo el formulario. Las dos situaciones se veían acá.
 */
export const AUTH_RULES = {
  /** `@Dni`: 7 u 8 dígitos. Los documentos anteriores a 1970 tienen 7. */
  dni: /^\d{7,8}$/,

  /** `@Username`: letras, dígitos y . _ - como separadores no repetidos ni en los extremos. */
  username: /^(?=.*[A-Za-z])[A-Za-z0-9]+([._-][A-Za-z0-9]+)*$/,
  usernameMinLength: 3,
  usernameMaxLength: 25,

  /** `@PersonName`: letras con acentos, y espacio, apóstrofo o guion como separadores. */
  personName: /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+([ '\-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$/,
  personNameMinLength: 2,
  personNameMaxLength: 50,

  passwordMinLength: 8,
  /** BCrypt ignora lo que pase de 72 bytes, así que el backend lo rechaza en vez de truncar. */
  passwordMaxLength: 72,
} as const;

/**
 * Contraseñas que cumplen los requisitos de composición y siguen siendo triviales.
 * Réplica de la lista de `StrongPasswordValidator`.
 */
const COMMON_PASSWORDS = new Set([
  'password1!', 'password123!', 'passw0rd!', 'qwerty123!', 'qwerty1234!',
  'abcd1234!', 'admin123!', 'arcash123!', 'argentina1!', 'contrasena1!',
  'contraseña1!', '1qaz2wsx!', 'iloveyou1!', 'welcome1!', 'aa123456!',
]);

/** Longitud mínima de un dato de identidad para compararlo por inclusión (ver backend). */
const MIN_IDENTITY_TERM_LENGTH = 4;

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

/**
 * Contraseña fuerte, con las mismas reglas que `@StrongPassword` en el backend.
 *
 * Devuelve una clave por incumplimiento para que el template pueda listar exactamente qué
 * falta, en lugar de un "contraseña insegura" que obliga al usuario a adivinar.
 */
export function strongPasswordValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (!value) {
    return null;
  }

  const errors: ValidationErrors = {};

  if (value.length < AUTH_RULES.passwordMinLength) {
    errors['minLength'] = true;
  }
  if (value.length > AUTH_RULES.passwordMaxLength) {
    errors['maxLength'] = true;
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
  // Cualquier carácter no alfanumérico cuenta, igual que en el backend. La lista corta de
  // símbolos que había acá rechazaba contraseñas que el servidor sí aceptaba.
  if (!/[^A-Za-z0-9]/.test(value)) {
    errors['specialChar'] = true;
  }
  if (/\s/.test(value)) {
    errors['whitespace'] = true;
  }
  if (COMMON_PASSWORDS.has(value.toLowerCase())) {
    errors['common'] = true;
  }

  return Object.keys(errors).length > 0 ? errors : null;
}

/**
 * Impide que la contraseña reproduzca los datos de identidad del propio usuario.
 *
 * Réplica de `@PasswordNotSimilarToIdentity`. Va sobre el formulario completo y no sobre el
 * campo porque necesita comparar la contraseña con el alias, el email, el DNI y el nombre, que
 * viven en otros controles. Sin esto el backend rechazaba el registro recién al enviarlo.
 *
 * @param identityPaths rutas de los controles de identidad, relativas al form raíz.
 * @param passwordPath ruta del control de la contraseña.
 */
export function passwordNotSimilarToIdentityValidator(
  identityPaths: string[],
  passwordPath = 'passwords.password'
) {
  return (form: AbstractControl): ValidationErrors | null => {
    const password = form.get(passwordPath)?.value;
    if (typeof password !== 'string' || !password.trim()) {
      return null;
    }

    const normalized = password.toLowerCase();

    for (const path of identityPaths) {
      const raw = form.get(path)?.value;
      if (typeof raw !== 'string' || !raw.trim()) {
        continue;
      }

      const term = raw.trim().toLowerCase();
      if (resembles(normalized, term)) {
        return { passwordLikeIdentity: { field: path } };
      }

      // El email se compara además por su parte local: para la cuenta "juan.perez@mail.com"
      // la contraseña "juan.perez" es igual de adivinable.
      const at = term.indexOf('@');
      if (at > 0 && resembles(normalized, term.slice(0, at))) {
        return { passwordLikeIdentity: { field: path } };
      }
    }

    return null;
  };
}

/**
 * PIN de 6 dígitos con las mismas exclusiones que `@ValidCardPin`.
 *
 * Solo se usa al crearlo: un PIN ya guardado puede ser débil (datos viejos) y el usuario
 * igual tiene que poder desbloquear. La lista negra y las secuencias son las que más se
 * eligen, y el backend las rechaza; sin este control el formulario enviaría 123456.
 */
export function cardPinValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (value == null || value === '') {
    return null;
  }
  return cardPinIssues(String(value));
}

/** Motivo por el que un PIN no sirve, o null si es aceptable. */
export function cardPinIssues(pin: string): ValidationErrors | null {
  if (!/^\d{6}$/.test(pin)) {
    return { pinFormat: true };
  }
  if (new Set(pin).size === 1) {
    return { pinRepeated: true };
  }
  if (isSequentialPin(pin)) {
    return { pinSequential: true };
  }
  if (COMMON_PINS.has(pin)) {
    return { pinCommon: true };
  }
  return null;
}

const COMMON_PINS = new Set([
  '123123', '121212', '112233', '123321', '696969',
  '159753', '147258', '102030', '123654', '789456',
]);

function isSequentialPin(pin: string): boolean {
  let ascending = true;
  let descending = true;
  for (let i = 1; i < pin.length; i++) {
    const delta = pin.charCodeAt(i) - pin.charCodeAt(i - 1);
    if (delta !== 1) {
      ascending = false;
    }
    if (delta !== -1) {
      descending = false;
    }
  }
  return ascending || descending;
}

function resembles(password: string, term: string): boolean {
  if (!term) {
    return false;
  }
  if (password === term) {
    return true;
  }
  // Los términos cortos darían falsos positivos: un apellido de dos letras aparece por
  // casualidad en casi cualquier contraseña.
  return term.length >= MIN_IDENTITY_TERM_LENGTH && password.includes(term);
}
