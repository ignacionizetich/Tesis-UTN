package com.EDJ.ArCash.exception.personalizated;

/**
 * Excepción lanzada cuando hay un conflicto con el estado actual del recurso
 * Por ejemplo: email duplicado, alias duplicado, etc.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
