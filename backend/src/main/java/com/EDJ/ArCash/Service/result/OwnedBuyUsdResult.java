package com.EDJ.ArCash.Service.result;

import java.util.Map;

public final class OwnedBuyUsdResult {

    public enum Kind {
        OK,
        ARS_NOT_FOUND,
        FORBIDDEN,
        FAIL
    }

    private final Kind kind;
    private final BuyUsdResult result;
    private final String message;

    private OwnedBuyUsdResult(Kind kind, BuyUsdResult result, String message) {
        this.kind = kind;
        this.result = result;
        this.message = message;
    }

    public static OwnedBuyUsdResult ok(BuyUsdResult result) {
        return new OwnedBuyUsdResult(Kind.OK, result, null);
    }

    public static OwnedBuyUsdResult arsNotFound() {
        return new OwnedBuyUsdResult(Kind.ARS_NOT_FOUND, null, "Cuenta en pesos no encontrada");
    }

    public static OwnedBuyUsdResult forbidden() {
        return new OwnedBuyUsdResult(Kind.FORBIDDEN, null, "No tiene permiso para operar esta cuenta");
    }

    public static OwnedBuyUsdResult fail(BuyUsdResult result) {
        return new OwnedBuyUsdResult(Kind.FAIL, result, null);
    }

    public Kind getKind() {
        return kind;
    }

    public BuyUsdResult getResult() {
        return result;
    }

    public String getMessage() {
        return result != null ? result.getMessage() : message;
    }

    public Map<String, Object> toErrorBody() {
        return Map.of("success", false, "message", getMessage());
    }
}
