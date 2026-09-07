package com.EDJ.ArCash.exception.personalizated;

/**
 * Fallo del servidor que el dominio ya detecto y quiere reportar con un mensaje propio.
 *
 * <p>Se diferencia del catch-all de {@code Exception}: aca la causa se conocia y el mensaje es
 * apto para mostrar al usuario, en lugar del texto genérico que se usa cuando la excepcion es
 * inesperada y podria filtrar detalles internos.
 */
public class InternalServerException extends RuntimeException {

    public InternalServerException(String message) {
        super(message);
    }

    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
