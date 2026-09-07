package com.EDJ.ArCash.exception.response;

import org.springframework.http.HttpStatus;

/**
 * Catalogo de errores de la API.
 *
 * <p>El {@code name()} de cada constante viaja al cliente en el campo {@code code} del
 * {@link ErrorResponse} y es la parte estable del contrato: el frontend puede ramificar por
 * codigo sin depender del texto en castellano, que esta pensado para mostrarse al usuario y
 * puede reescribirse en cualquier momento.
 *
 * <p>Cada codigo declara su {@link HttpStatus} para que el estado HTTP y el codigo no puedan
 * quedar desalineados entre distintos {@code @ExceptionHandler}.
 */
public enum ApiErrorCode {

    // --- 400 Bad Request ---
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    MALFORMED_JSON(HttpStatus.BAD_REQUEST),
    TYPE_MISMATCH(HttpStatus.BAD_REQUEST),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST),
    ILLEGAL_ARGUMENT(HttpStatus.BAD_REQUEST),
    CREDENTIALS_MISSMATCH(HttpStatus.BAD_REQUEST),

    // --- 401 Unauthorized ---
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED),
    AUTHENTICATION_ERROR(HttpStatus.UNAUTHORIZED),
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED),
    SESSION_ENDED(HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(HttpStatus.UNAUTHORIZED),

    // --- 403 Forbidden ---
    FORBIDDEN(HttpStatus.FORBIDDEN),
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    NOT_ACCOUNT_OWNER(HttpStatus.FORBIDDEN),
    CARD_LOCKED_NEEDS_PIN(HttpStatus.FORBIDDEN),

    // --- 404 Not Found ---
    NOT_FOUND(HttpStatus.NOT_FOUND),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND),

    // --- 405 / 409 / 415 / 423 ---
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    CONFLICT(HttpStatus.CONFLICT),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    CARD_PIN_LOCKED(HttpStatus.LOCKED),

    // --- 5xx ---
    EMAIL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    ENCODING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    EXCHANGE_RATE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE);

    private final HttpStatus status;

    ApiErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
