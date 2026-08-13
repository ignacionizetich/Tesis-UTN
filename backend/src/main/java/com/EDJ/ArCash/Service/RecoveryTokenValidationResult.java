package com.EDJ.ArCash.Service;

import java.util.Map;

/**
 * Validacion de token de recuperacion (GET validate-recovery-token).
 */
public final class RecoveryTokenValidationResult {

    public enum Kind {
        VALID,
        INVALID,
        ERROR
    }

    private final Kind kind;
    private final boolean valid;
    private final String message;

    private RecoveryTokenValidationResult(Kind kind, boolean valid, String message) {
        this.kind = kind;
        this.valid = valid;
        this.message = message;
    }

    public static RecoveryTokenValidationResult valid() {
        return new RecoveryTokenValidationResult(Kind.VALID, true, "Enlace de recuperación válido");
    }

    public static RecoveryTokenValidationResult invalid() {
        return new RecoveryTokenValidationResult(Kind.INVALID, false,
                "El enlace de recuperación es inválido, ha expirado o ya fue utilizado");
    }

    public static RecoveryTokenValidationResult error() {
        return new RecoveryTokenValidationResult(Kind.ERROR, false,
                "Error al validar el enlace de recuperación");
    }

    public Kind getKind() {
        return kind;
    }

    public Map<String, Object> toBody() {
        return Map.of("valid", valid, "message", message);
    }
}
