package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.DTO.NonAuthDTO.RegisterResponse;

/**
 * Registro publico tipado (POST /api/user/create).
 */
public final class RegisterResult {

    public enum Kind {
        OK,
        VALIDATION,
        CONFLICT,
        ERROR
    }

    private final Kind kind;
    private final String message;

    private RegisterResult(Kind kind, String message) {
        this.kind = kind;
        this.message = message;
    }

    public static RegisterResult ok() {
        return new RegisterResult(Kind.OK,
                "Usuario registrado correctamente. Revisa tu email para activar tu cuenta.");
    }

    public static RegisterResult validation(String message) {
        return new RegisterResult(Kind.VALIDATION, message);
    }

    public static RegisterResult conflict(String message) {
        return new RegisterResult(Kind.CONFLICT, message);
    }

    public static RegisterResult error() {
        return new RegisterResult(Kind.ERROR, "Error interno del servidor.");
    }

    public Kind getKind() {
        return kind;
    }

    public RegisterResponse toResponse() {
        return new RegisterResponse(kind == Kind.OK, message);
    }
}
