package com.EDJ.ArCash.exception.personalizated;

/**
 * Excepción lanzada cuando hay un conflicto con el estado actual del recurso
 * Por ejemplo: email duplicado, alias duplicado, etc.
 */
public class ConflictException extends RuntimeException {

    /**
     * Campo que provoco el conflicto, o {@code null} si no se puede atribuir a uno solo.
     *
     * <p>Permite que el frontend marque el input culpable en lugar de mostrar solo un toast:
     * el handler lo publica en {@code fieldErrors}, el mismo lugar donde aparecen los errores
     * de Bean Validation, asi el cliente lo trata igual sin importar de donde venga.
     */
    private final String field;

    public ConflictException(String message) {
        this(message, null);
    }

    public ConflictException(String message, String field) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
