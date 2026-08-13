package com.EDJ.ArCash.Service;

/**
 * Venta USD con ownership de la cuenta USD origen.
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

    private OwnedSellUsdResult(Kind kind, SellUsdResult result) {
        this.kind = kind;
        this.result = result;
    }

    public static OwnedSellUsdResult ok(SellUsdResult result) {
        return new OwnedSellUsdResult(Kind.OK, result);
    }

    public static OwnedSellUsdResult usdNotFound() {
        return new OwnedSellUsdResult(Kind.USD_NOT_FOUND, null);
    }

    public static OwnedSellUsdResult forbidden() {
        return new OwnedSellUsdResult(Kind.FORBIDDEN, null);
    }

    public static OwnedSellUsdResult fail(SellUsdResult result) {
        return new OwnedSellUsdResult(Kind.FAIL, result);
    }

    public Kind getKind() {
        return kind;
    }

    public SellUsdResult getResult() {
        return result;
    }
}
