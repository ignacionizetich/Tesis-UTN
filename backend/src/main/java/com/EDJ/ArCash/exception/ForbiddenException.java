package com.EDJ.ArCash.exception;

/**
 * Excepción lanzada cuando el usuario no tiene permisos para acceder al recurso
 */
public class ForbiddenException extends RuntimeException {
    
    public ForbiddenException(String message) {
        super(message);
    }
}
