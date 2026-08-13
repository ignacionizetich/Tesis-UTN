package com.EDJ.ArCash.Service;

import java.util.Map;

/**
 * Venta USD con ownership de la cuenta USD origen.
 * Mensajes de dominio viven aca; el controller solo mapea Kind → HTTP.
 * principal==null → 401 queda en el controller.
 */
public final class OwnedSellUsdResult {

    public enum Kind {
        OK,
        USD_NOT_FOUND,
        FORBIDDEN,
        FAIL
    }

    private final Kind kind;
    private final SellUsdResult result;
    private final String message;

    private OwnedSellUsdResult(Kind kind, SellUsdResult result, String message) {
        this.kind = kind;
        this.result = result;
        this.message = message;
    }

    public static OwnedSellUsdResult ok(SellUsdResult result) {
        return new OwnedSellUsdResult(Kind.OK, result, null);
    }

    public static OwnedSellUsdResult usdNotFound() {
        return new OwnedSellUsdResult(Kind.USD_NOT_FOUND, null, "Cuenta en dólares no encontrada");
    }

    public static OwnedSellUsdResult forbidden() {
        return new OwnedSellUsdResult(Kind.FORBIDDEN, null, "No tiene permiso para operar esta cuenta");
    }

    public static OwnedSellUsdResult fail(SellUsdResult result) {
        return new OwnedSellUsdResult(Kind.FAIL, result, null);
    }

    public Kind getKind() {
        return kind;
    }

    public SellUsdResult getResult() {
        return result;
    }

    public String getMessage() {
        return result != null ? result.getMessage() : message;
    }

    public Map<String, Object> toErrorBody() {
        return Map.of("success", false, "message", getMessage());
    }
}
