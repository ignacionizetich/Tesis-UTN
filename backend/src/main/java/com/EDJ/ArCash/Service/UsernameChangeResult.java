package com.EDJ.ArCash.Service;

/**
 * Resultado de PUT /api/auth/changeUsername.
 */
public final class UsernameChangeResult {

    public enum Kind {
        OK,
        EMPTY,
        FAIL
    }

    private final Kind kind;
    private final String message;

    private UsernameChangeResult(Kind kind, String message) {
        this.kind = kind;
        this.message = message;
    }

    public static UsernameChangeResult ok() {
        return new UsernameChangeResult(Kind.OK, "Nombre de usuario actualizado correctamente");
    }

    public static UsernameChangeResult empty() {
        return new UsernameChangeResult(Kind.EMPTY, "El nombre de usuario no puede estar vacio");
    }

    public static UsernameChangeResult fail() {
        return new UsernameChangeResult(Kind.FAIL,
                "No se pudo actualizar el nombre de usuario. Puede que ya exista.");
    }

    public Kind getKind() {
        return kind;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return kind == Kind.OK;
    }
}
