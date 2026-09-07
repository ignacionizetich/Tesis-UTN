package com.EDJ.ArCash.Service.result;

import com.EDJ.ArCash.DTO.common.ApiMessageResponse;

public final class ResendEmailResult {

    public enum Kind {
        OK,
        BAD_REQUEST,
        ERROR
    }

    private final Kind kind;
    private final String message;

    private ResendEmailResult(Kind kind, String message) {
        this.kind = kind;
        this.message = message;
    }

    public static ResendEmailResult ok(String message) {
        return new ResendEmailResult(Kind.OK, message);
    }

    public static ResendEmailResult emailRequired() {
        return new ResendEmailResult(Kind.BAD_REQUEST, "El email es requerido.");
    }

    public static ResendEmailResult error() {
        return new ResendEmailResult(Kind.ERROR, "Error interno del servidor. Inténtalo de nuevo.");
    }

    public Kind getKind() {
        return kind;
    }

    public ApiMessageResponse toBody() {
        return new ApiMessageResponse(kind == Kind.OK, message);
    }
}
