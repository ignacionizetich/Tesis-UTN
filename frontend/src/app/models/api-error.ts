/**
 * Espejo del cuerpo de error que devuelve el backend (`ErrorResponse`).
 *
 * Todos los `@ExceptionHandler` y los filtros de seguridad responden con esta forma, así que
 * el frontend tiene un solo contrato que interpretar. La clave es `code`: es estable y pensada
 * para que el cliente ramifique por ella, mientras `message` está redactado para mostrarse al
 * usuario y puede reescribirse en cualquier momento sin romper nada.
 */
export interface ApiErrorBody {
  success: false;
  status: number;
  code: string;
  message: string;
  path?: string;
  method?: string;
  /** Correlativo que también queda en el log del servidor, útil para reportar un error. */
  traceId?: string;
  timestamp?: string;
  /** Presente solo cuando el error viene de validación de campos. */
  fieldErrors?: ApiFieldError[];
}

/** Un error atribuible a un campo puntual del request. */
export interface ApiFieldError {
  field: string;
  message: string;
}

/**
 * Códigos que el frontend necesita distinguir para tomar una decisión distinta.
 *
 * El backend define muchos más en `ApiErrorCode`; acá solo están los que cambian el
 * comportamiento del cliente. Para el resto alcanza con mostrar `message`.
 */
export const ApiErrorCodes = {
  /** La cuenta está deshabilitada: no tiene sentido reintentar ni renovar el token. */
  ACCOUNT_DISABLED: 'ACCOUNT_DISABLED',
  /** Falló Bean Validation: viene con `fieldErrors` para marcar los campos del formulario. */
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  CONFLICT: 'CONFLICT',
  NOT_FOUND: 'NOT_FOUND',
  /** El proveedor de cotización no respondió. */
  EXCHANGE_RATE_UNAVAILABLE: 'EXCHANGE_RATE_UNAVAILABLE',
  /** La tarjeta quedó bloqueada por PIN incorrecto. */
  CARD_PIN_LOCKED: 'CARD_PIN_LOCKED',
  /** Hace falta desbloquear con PIN antes de ver los datos. */
  CARD_LOCKED_NEEDS_PIN: 'CARD_LOCKED_NEEDS_PIN',
} as const;

export type ApiErrorCode = (typeof ApiErrorCodes)[keyof typeof ApiErrorCodes];
