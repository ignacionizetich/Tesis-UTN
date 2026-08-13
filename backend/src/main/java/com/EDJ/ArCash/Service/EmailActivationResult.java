package com.EDJ.ArCash.Service;

/**
 * Resultado tipado de GET /api/auth/validate (activacion por email).
 */
public final class EmailActivationResult {

    public enum Kind {
        MISSING_TOKEN,
        INVALID,
        ALREADY_USED,
        EXPIRED,
        OK
    }

    private final Kind kind;
    private final String message;

    private EmailActivationResult(Kind kind, String message) {
        this.kind = kind;
        this.message = message;
    }

    public static EmailActivationResult missingToken() {
        return new EmailActivationResult(Kind.MISSING_TOKEN, "Token no proporcionado");
    }

    public static EmailActivationResult invalid() {
        return new EmailActivationResult(Kind.INVALID,
                "El enlace de verificación no es válido o no existe.");
    }

    public static EmailActivationResult alreadyUsed() {
        return new EmailActivationResult(Kind.ALREADY_USED,
                "Este enlace de verificación ya fue utilizado. Tu cuenta ya está activada.");
    }

    public static EmailActivationResult expired() {
        return new EmailActivationResult(Kind.EXPIRED,
                "El enlace de verificación ha expirado. Solicita un nuevo enlace de activación.");
    }

    public static EmailActivationResult ok() {
        return new EmailActivationResult(Kind.OK,
                "¡Cuenta verificada exitosamente! Ya puedes iniciar sesión.");
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
