package com.EDJ.ArCash.Service;

import java.util.Map;

/**
 * Update de favorito: incluye la regla "al menos un campo".
 */
public final class FavoriteUpdateResult {

    public enum Kind {
        OK,
        BAD_REQUEST,
        NOT_FOUND
    }

    private final Kind kind;
    private final String message;

    private FavoriteUpdateResult(Kind kind, String message) {
        this.kind = kind;
        this.message = message;
    }

    public static FavoriteUpdateResult ok() {
        return new FavoriteUpdateResult(Kind.OK, "Contacto favorito actualizado correctamente");
    }

    public static FavoriteUpdateResult badRequest() {
        return new FavoriteUpdateResult(Kind.BAD_REQUEST,
                "Debe proporcionar al menos un campo para actualizar");
    }

    public static FavoriteUpdateResult notFound() {
        return new FavoriteUpdateResult(Kind.NOT_FOUND,
                "No se pudo actualizar el contacto. Verifique que existe y le pertenece.");
    }

    public Kind getKind() {
        return kind;
    }

    public Map<String, String> toBody(String status) {
        return Map.of("status", status, "message", message);
    }
}
