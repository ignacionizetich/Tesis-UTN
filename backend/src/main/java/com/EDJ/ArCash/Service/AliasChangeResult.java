package com.EDJ.ArCash.Service;

/**
 * Desenlaces posibles de un cambio de alias de cuenta.
 * Mensajes de dominio aca; el controller mapea a HTTP + AliasResponse.
 */
public enum AliasChangeResult {
    OK("Alias actualizado exitosamente."),
    FORMATO_INVALIDO(
            "Formato de alias inválido. Debe tener entre 4 y 25 caracteres, solo letras, números y puntos, al menos un punto en el medio, no puede ser solo números ni tener '..'."),
    CUENTA_NO_ENCONTRADA("Cuenta no encontrada."),
    NO_ES_PROPIETARIO("No tienes permisos para hacer eso."),
    ALIAS_EN_USO("Alias actualmente en uso.");

    private final String message;

    AliasChangeResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
