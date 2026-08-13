package com.EDJ.ArCash.exception.personalizated;

/**
 * Excepción lanzada cuando el usuario no está autorizado
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
