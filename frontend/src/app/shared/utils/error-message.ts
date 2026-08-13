import { HttpErrorResponse } from '@angular/common/http';

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
