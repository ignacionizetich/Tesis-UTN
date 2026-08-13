import { environment } from '../../../environments/environment';

/**
 * Logger mínimo: errors siempre; warn/debug solo fuera de producción.
 */
export const logger = {
  error(...args: unknown[]): void {
    console.error(...args);
  },

  warn(...args: unknown[]): void {
    if (!environment.production) {
      console.warn(...args);
    }
  },

  debug(...args: unknown[]): void {
    if (!environment.production) {
      console.debug(...args);
    }
  },
};
