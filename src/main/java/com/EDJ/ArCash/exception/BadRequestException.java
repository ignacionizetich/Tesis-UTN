package com.EDJ.ArCash.exception;

/**
 * Excepción lanzada cuando los datos de la petición son inválidos
 */
public class BadRequestException extends RuntimeException {
    
    public BadRequestException(String message) {
        super(message);
    }
}
