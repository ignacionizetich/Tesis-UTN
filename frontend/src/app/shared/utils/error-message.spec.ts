import { errorMessage, httpStatus } from './error-message';
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
});
