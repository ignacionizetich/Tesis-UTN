package com.EDJ.ArCash.Service;

/**
 * Resultado tipado de POST /api/auth/reset-password.
 * El controller mapea Kind → HTTP sin heuristica de substrings.
 */
public final class ResetPasswordResult {

    public enum Kind {
        OK,
        UNAUTHORIZED,
        BAD_REQUEST
    }

    private final Kind kind;
    private final String message;

    private ResetPasswordResult(Kind kind, String message) {
        this.kind = kind;
        this.message = message;
    }

    public static ResetPasswordResult ok(String message) {
        return new ResetPasswordResult(Kind.OK, message);
    }

    public static ResetPasswordResult unauthorized(String message) {
        return new ResetPasswordResult(Kind.UNAUTHORIZED, message);
    }

    public static ResetPasswordResult badRequest(String message) {
        return new ResetPasswordResult(Kind.BAD_REQUEST, message);
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
