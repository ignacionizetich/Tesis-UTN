package com.EDJ.ArCash.Service;

/**
 * Compra USD con ownership de la cuenta ARS origen.
 * principal==null → 401 queda en el controller.
 */
public final class OwnedBuyUsdResult {

    public enum Kind {
        OK,
        ARS_NOT_FOUND,
        FORBIDDEN,
        FAIL
    }

    private final Kind kind;
    private final BuyUsdResult result;

    private OwnedBuyUsdResult(Kind kind, BuyUsdResult result) {
        this.kind = kind;
        this.result = result;
    }

    public static OwnedBuyUsdResult ok(BuyUsdResult result) {
        return new OwnedBuyUsdResult(Kind.OK, result);
    }

    public static OwnedBuyUsdResult arsNotFound() {
        return new OwnedBuyUsdResult(Kind.ARS_NOT_FOUND, null);
    }

    public static OwnedBuyUsdResult forbidden() {
        return new OwnedBuyUsdResult(Kind.FORBIDDEN, null);
    }

    public static OwnedBuyUsdResult fail(BuyUsdResult result) {
        return new OwnedBuyUsdResult(Kind.FAIL, result);
    }

    public Kind getKind() {
        return kind;
    }

    public BuyUsdResult getResult() {
        return result;
    }
}
