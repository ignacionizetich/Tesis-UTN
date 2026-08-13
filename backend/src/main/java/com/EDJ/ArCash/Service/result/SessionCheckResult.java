package com.EDJ.ArCash.Service.result;

import java.util.Map;

/**
 * Resultado de GET /api/auth/check-session.
 */
public final class SessionCheckResult {

    public enum Kind {
        ACTIVE,
        INACTIVE,
        ERROR
    }

    private final Kind kind;
    private final String status;
    private final String message;

    private SessionCheckResult(Kind kind, String status, String message) {
        this.kind = kind;
        this.status = status;
        this.message = message;
    }

    public static SessionCheckResult active() {
        return new SessionCheckResult(Kind.ACTIVE, "ACTIVE", "Sesión activa");
    }

    public static SessionCheckResult inactive() {
        return new SessionCheckResult(Kind.INACTIVE, "INACTIVE", "No hay sesión activa");
    }

    public static SessionCheckResult error() {
        return new SessionCheckResult(Kind.ERROR, "ERROR", "Error al verificar la sesión");
    }

    public Kind getKind() {
        return kind;
    }

    public Map<String, String> toBody() {
        return Map.of("status", status, "message", message);
    }
}
