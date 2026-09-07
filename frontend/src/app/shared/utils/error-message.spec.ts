import { apiError, apiErrorCode, errorMessage, fieldErrorsOf, httpStatus } from './error-message';
import { HttpErrorResponse } from '@angular/common/http';

describe('errorMessage utils', () => {
  it('usa message de Error', () => {
    expect(errorMessage(new Error('boom'), 'fallback')).toBe('boom');
  });

  it('usa message del body HTTP', () => {
    const err = new HttpErrorResponse({
      status: 400,
      error: { message: 'Datos inválidos' },
    });
    expect(errorMessage(err, 'fallback')).toBe('Datos inválidos');
    expect(httpStatus(err)).toBe(400);
  });

  it('cae a fallback', () => {
    expect(errorMessage('x', 'fallback')).toBe('fallback');
    expect(httpStatus('x')).toBeUndefined();
  });

  it('no muestra el texto crudo de Angular cuando el error no trae body', () => {
    // Sin esto el usuario vería "Http failure response for /api/loans: 500 ...".
    const err = new HttpErrorResponse({ status: 500, url: '/api/loans' });
    expect(errorMessage(err, 'No se pudo completar la operación')).toBe(
      'No se pudo completar la operación'
    );
  });
});

describe('apiError', () => {
  const errorResponse = (body: unknown, status = 400) =>
    new HttpErrorResponse({ status, error: body });

  it('reconoce el cuerpo de error estándar del backend', () => {
    const err = errorResponse({
      success: false,
      status: 409,
      code: 'CONFLICT',
      message: 'El email ya se encuentra en uso',
      traceId: 'abc-123',
    }, 409);

    expect(apiError(err)?.code).toBe('CONFLICT');
    expect(apiErrorCode(err)).toBe('CONFLICT');
    expect(apiError(err)?.traceId).toBe('abc-123');
  });

  it('devuelve null si el error no viene de la API', () => {
    // Fallo de red: status 0 y sin body. Hay que poder distinguirlo de un rechazo del server.
    expect(apiError(new HttpErrorResponse({ status: 0 }))).toBeNull();
    expect(apiError(new Error('offline'))).toBeNull();
    expect(apiErrorCode(new Error('offline'))).toBeUndefined();
  });

  it('devuelve null si el body no tiene la forma esperada', () => {
    expect(apiError(errorResponse('texto plano'))).toBeNull();
    expect(apiError(errorResponse({ message: 'sin code' }))).toBeNull();
  });

  it('expone los errores por campo de una validación', () => {
    const err = errorResponse({
      success: false,
      status: 400,
      code: 'VALIDATION_ERROR',
      message: 'Hay campos inválidos',
      fieldErrors: [
        { field: 'dni', message: 'debe tener 8 dígitos' },
        { field: 'password', message: 'es demasiado débil' },
      ],
    });

    expect(fieldErrorsOf(err).map((f) => f.field)).toEqual(['dni', 'password']);
  });

  it('devuelve lista vacía cuando el error no trae fieldErrors', () => {
    const err = errorResponse({
      success: false,
      status: 404,
      code: 'NOT_FOUND',
      message: 'No encontrado',
    });

    expect(fieldErrorsOf(err)).toEqual([]);
    expect(fieldErrorsOf(new Error('x'))).toEqual([]);
  });
});
