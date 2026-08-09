package com.EDJ.ArCash.Service;

/**
 * Resultado de crear un administrador (sin ResponseEntity ni detalle de DB).
 */
public final class AdminCreateResult {

    public enum Kind {
        SUCCESS,
        CONFLICT
    }

    private final Kind kind;
    private final String mensaje;
    private final String campo;

    private AdminCreateResult(Kind kind, String mensaje, String campo) {
        this.kind = kind;
        this.mensaje = mensaje;
        this.campo = campo;
    }

    public static AdminCreateResult success() {
        return new AdminCreateResult(Kind.SUCCESS, null, null);
    }

    public static AdminCreateResult conflict(String campo, String mensaje) {
        return new AdminCreateResult(Kind.CONFLICT, mensaje, campo);
    }

    /** Conflicto de carrera / constraint sin campo mapeable. */
    public static AdminCreateResult conflictGeneric() {
        return new AdminCreateResult(Kind.CONFLICT, "Error de duplicación en la base de datos", null);
    }

    public Kind getKind() {
        return kind;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getCampo() {
        return campo;
    }

    public boolean isSuccess() {
        return kind == Kind.SUCCESS;
    }
}
