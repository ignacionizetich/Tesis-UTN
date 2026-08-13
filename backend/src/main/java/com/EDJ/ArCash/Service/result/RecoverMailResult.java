package com.EDJ.ArCash.Service.result;

import java.util.Map;

/**
 * Resultado de POST /api/auth/send-recover-mail.
 */
public final class RecoverMailResult {

    public enum Kind {
        OK,
        NOT_FOUND,
        ERROR
    }

    private final Kind kind;
    private final String message;

    private RecoverMailResult(Kind kind, String message) {
        this.kind = kind;
        this.message = message;
    }

    public static RecoverMailResult ok() {
        return new RecoverMailResult(Kind.OK, "Correo enviado correctamente");
    }

    public static RecoverMailResult notFound() {
        return new RecoverMailResult(Kind.NOT_FOUND,
                "El correo ingresado no se asocia a una cuenta existente.");
    }

    public static RecoverMailResult error() {
        return new RecoverMailResult(Kind.ERROR, "Error interno al enviar el correo");
    }

    public Kind getKind() {
        return kind;
    }

    public Map<String, String> toBody() {
        return Map.of("message", message);
    }
}
