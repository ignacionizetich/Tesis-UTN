package com.EDJ.ArCash.Service;

/**
 * Resultado tipado de POST /api/auth/refresh.
 * Cookie ausente → MISSING (400); token invalido/expirado → INVALID (401).
 */
public final class RefreshAccessResult {

    public enum Kind {
        MISSING,
        INVALID,
        OK
    }

    private final Kind kind;
    private final String accessToken;
    private final String error;

    private RefreshAccessResult(Kind kind, String accessToken, String error) {
        this.kind = kind;
        this.accessToken = accessToken;
        this.error = error;
    }

    public static RefreshAccessResult missing() {
        return new RefreshAccessResult(Kind.MISSING, null, "Refresh token requerido");
    }

    public static RefreshAccessResult invalid() {
        return new RefreshAccessResult(Kind.INVALID, null, "Refresh token inválido o expirado");
    }

    public static RefreshAccessResult ok(String accessToken) {
        return new RefreshAccessResult(Kind.OK, accessToken, null);
    }

    public Kind getKind() {
        return kind;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getError() {
        return error;
    }
}
