package com.EDJ.ArCash.Service;

/**
 * Transferencia iniciada por un usuario autenticado (ownership + negocio).
 * El controller mapea a HTTP; principal==null sigue en el controller (498).
 */
public final class OwnedTransferResult {

    public enum Kind {
        OK,
        ACCOUNT_NOT_FOUND,
        FORBIDDEN,
        FAIL
    }

    private final Kind kind;
    private final String message;

    private OwnedTransferResult(Kind kind, String message) {
        this.kind = kind;
        this.message = message;
    }

    public static OwnedTransferResult ok() {
        return new OwnedTransferResult(Kind.OK, "Transferencia realizada correctamente");
    }

    public static OwnedTransferResult accountNotFound() {
        return new OwnedTransferResult(Kind.ACCOUNT_NOT_FOUND, "No se pudo encontrar la cuenta de orig.");
    }

    public static OwnedTransferResult forbidden() {
        return new OwnedTransferResult(Kind.FORBIDDEN, "No tiene permiso para operar esta cuenta");
    }

    public static OwnedTransferResult fail(String message) {
        return new OwnedTransferResult(Kind.FAIL,
                message != null ? message : "Not enough cash, stranger.");
    }

    public Kind getKind() {
        return kind;
    }

    public String getMessage() {
        return message;
    }
}
