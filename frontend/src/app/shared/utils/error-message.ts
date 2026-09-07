import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorBody, ApiFieldError } from '../../models/api-error';

/**
 * Reconoce el cuerpo de error estándar del backend.
 *
 * Devuelve null cuando el error no viene de la API (un fallo de red, por ejemplo, llega con
 * `status` 0 y el body vacío), así que quien llama puede distinguir "el servidor rechazó la
 * operación y explicó por qué" de "no pudimos hablar con el servidor".
 */
export function apiError(error: unknown): ApiErrorBody | null {
  if (!(error instanceof HttpErrorResponse)) {
    return null;
  }
  const body = error.error;
  if (!body || typeof body !== 'object') {
    return null;
  }
  const candidate = body as Partial<ApiErrorBody>;
  if (typeof candidate.code !== 'string' || typeof candidate.message !== 'string') {
    return null;
  }
  return candidate as ApiErrorBody;
}

/**
 * Código de error estable del backend, si lo hay.
 *
 * Preferí esto antes que comparar `message`: el texto está en castellano y pensado para el
 * usuario, así que reescribirlo no debería cambiar la lógica del cliente.
 */
export function apiErrorCode(error: unknown): string | undefined {
  return apiError(error)?.code;
}

/** Errores por campo de una respuesta de validación; vacío si el error no los trae. */
export function fieldErrorsOf(error: unknown): ApiFieldError[] {
  return apiError(error)?.fieldErrors ?? [];
}

/** Mensaje usable en toast a partir de Error / HttpErrorResponse. */
export function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error;
    if (body && typeof body === 'object' && 'message' in body) {
      const msg = (body as { message?: unknown }).message;
      if (typeof msg === 'string' && msg.trim()) {
        return msg;
      }
    }
  }
  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }
  return fallback;
}

export function httpStatus(error: unknown): number | undefined {
  return error instanceof HttpErrorResponse ? error.status : undefined;
}
