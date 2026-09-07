import { FormBuilder, FormGroup } from '@angular/forms';
import {
  AUTH_RULES,
  cardPinIssues,
  passwordNotSimilarToIdentityValidator,
  strongPasswordValidator,
} from './auth.validators';

/** Control mínimo con el valor a validar, para no depender de un FormGroup completo. */
const control = (value: unknown) => ({ value }) as any;

describe('strongPasswordValidator', () => {
  it('acepta una contraseña que cumple toda la política', () => {
    expect(strongPasswordValidator(control('Arcash2026$seguro'))).toBeNull();
  });

  it('no opina si el campo está vacío: de eso se encarga required', () => {
    expect(strongPasswordValidator(control(''))).toBeNull();
    expect(strongPasswordValidator(control(null))).toBeNull();
  });

  it('marca cada requisito que falta por separado', () => {
    // Un único "contraseña insegura" obligaría al usuario a adivinar qué corregir.
    expect(strongPasswordValidator(control('abc'))).toEqual({
      minLength: true,
      uppercase: true,
      number: true,
      specialChar: true,
    });
  });

  it('rechaza espacios, que el backend también rechaza', () => {
    expect(strongPasswordValidator(control('Arcash 2026$'))?.['whitespace']).toBeTrue();
  });

  it('rechaza contraseñas comunes aunque cumplan la composición', () => {
    // "Password1!" tiene mayúscula, minúscula, número y símbolo, y es de las primeras que
    // se prueban en un ataque de diccionario.
    expect(strongPasswordValidator(control('Password1!'))?.['common']).toBeTrue();
    expect(strongPasswordValidator(control('PASSWORD1!'))?.['common']).toBeTrue();
  });

  it('rechaza contraseñas más largas de lo que BCrypt considera', () => {
    const larga = 'A1$' + 'a'.repeat(AUTH_RULES.passwordMaxLength);
    expect(strongPasswordValidator(control(larga))?.['maxLength']).toBeTrue();
  });

  it('acepta cualquier símbolo como carácter especial, igual que el backend', () => {
    // La lista corta que había antes rechazaba contraseñas que el servidor sí aceptaba.
    expect(strongPasswordValidator(control('Arcash2026/ok'))).toBeNull();
    expect(strongPasswordValidator(control('Arcash2026~ok'))).toBeNull();
  });
});

describe('passwordNotSimilarToIdentityValidator', () => {
  let fb: FormBuilder;
  let form: FormGroup;

  const IDENTITY_PATHS = ['alias', 'emails.email', 'dni', 'nombre', 'apellido'];

  const build = (values: {
    alias?: string;
    email?: string;
    dni?: string;
    nombre?: string;
    apellido?: string;
    password?: string;
  }) => {
    form = fb.group(
      {
        alias: [values.alias ?? ''],
        dni: [values.dni ?? ''],
        nombre: [values.nombre ?? ''],
        apellido: [values.apellido ?? ''],
        emails: fb.group({ email: [values.email ?? ''] }),
        passwords: fb.group({ password: [values.password ?? ''] }),
      },
      { validators: passwordNotSimilarToIdentityValidator(IDENTITY_PATHS) }
    );
    return form;
  };

  beforeEach(() => {
    fb = new FormBuilder();
  });

  it('rechaza una contraseña igual al nombre de usuario', () => {
    const f = build({ alias: 'juan.perez', password: 'juan.perez' });
    expect(f.errors?.['passwordLikeIdentity']).toEqual({ field: 'alias' });
  });

  it('ignora las mayúsculas al comparar', () => {
    // "JUAN.PEREZ" no es más segura que "juan.perez".
    const f = build({ alias: 'juan.perez', password: 'JUAN.PEREZ' });
    expect(f.errors?.['passwordLikeIdentity']).toBeTruthy();
  });

  it('rechaza una contraseña que contiene el nombre de usuario', () => {
    // Es el patrón real con el que la gente esquiva una comparación por igualdad.
    const f = build({ alias: 'juan.perez', password: 'Juan.perez2026!' });
    expect(f.errors?.['passwordLikeIdentity']).toEqual({ field: 'alias' });
  });

  it('rechaza una contraseña igual a la parte local del email', () => {
    const f = build({ email: 'ana.gomez@mail.com', password: 'Ana.gomez1$' });
    expect(f.errors?.['passwordLikeIdentity']).toEqual({ field: 'emails.email' });
  });

  it('rechaza una contraseña que incluye el DNI', () => {
    const f = build({ dni: '30123456', password: 'Casa30123456$' });
    expect(f.errors?.['passwordLikeIdentity']).toEqual({ field: 'dni' });
  });

  it('acepta una contraseña que no se parece a ningún dato de identidad', () => {
    const f = build({
      alias: 'juan.perez',
      email: 'juan.perez@mail.com',
      dni: '30123456',
      nombre: 'Juan',
      apellido: 'Pérez',
      password: 'Arcash2026$seguro',
    });
    expect(f.errors).toBeNull();
  });

  it('no marca error por coincidencias cortas', () => {
    // Un apellido de dos letras aparece por casualidad en casi cualquier contraseña:
    // compararlo por inclusión daría un falso positivo.
    const f = build({ apellido: 'Li', password: 'Arcash2026$li' });
    expect(f.errors).toBeNull();
  });

  it('no opina mientras la contraseña está vacía', () => {
    const f = build({ alias: 'juan.perez', password: '' });
    expect(f.errors).toBeNull();
  });
});

describe('cardPinIssues', () => {
  it('acepta un PIN de 6 dígitos sin patrón trivial', () => {
    expect(cardPinIssues('847291')).toBeNull();
  });

  it('rechaza formato, dígitos iguales, secuencias y PIN de uso masivo', () => {
    expect(cardPinIssues('12345')?.['pinFormat']).toBeTrue();
    expect(cardPinIssues('111111')?.['pinRepeated']).toBeTrue();
    expect(cardPinIssues('123456')?.['pinSequential']).toBeTrue();
    expect(cardPinIssues('654321')?.['pinSequential']).toBeTrue();
    expect(cardPinIssues('112233')?.['pinCommon']).toBeTrue();
  });
});

describe('AUTH_RULES', () => {
  it('acepta los DNI de 7 y 8 dígitos que acepta el backend', () => {
    expect(AUTH_RULES.dni.test('1234567')).toBeTrue();
    expect(AUTH_RULES.dni.test('12345678')).toBeTrue();
    expect(AUTH_RULES.dni.test('123456')).toBeFalse();
    expect(AUTH_RULES.dni.test('12.345.678')).toBeFalse();
  });

  it('acepta nombres de usuario con puntos y rechaza separadores mal ubicados', () => {
    expect(AUTH_RULES.username.test('juan.perez.01')).toBeTrue();
    expect(AUTH_RULES.username.test('juan_perez')).toBeTrue();
    expect(AUTH_RULES.username.test('.juan')).toBeFalse();
    expect(AUTH_RULES.username.test('juan.')).toBeFalse();
    expect(AUTH_RULES.username.test('juan..perez')).toBeFalse();
    // Sin al menos una letra se confundiría con un número de cuenta.
    expect(AUTH_RULES.username.test('12345')).toBeFalse();
  });

  it('acepta apellidos con acentos, apóstrofos y guiones', () => {
    expect(AUTH_RULES.personName.test('Muñoz')).toBeTrue();
    expect(AUTH_RULES.personName.test("O'Brien")).toBeTrue();
    expect(AUTH_RULES.personName.test('Díaz-López')).toBeTrue();
    expect(AUTH_RULES.personName.test('Juan Carlos')).toBeTrue();
    expect(AUTH_RULES.personName.test(' Juan')).toBeFalse();
    expect(AUTH_RULES.personName.test('Juan2')).toBeFalse();
  });
});
