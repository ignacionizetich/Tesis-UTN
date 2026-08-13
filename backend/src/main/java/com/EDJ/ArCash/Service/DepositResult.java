package com.EDJ.ArCash.Service;

/**
 * Resultado de un ingreso de dinero. El controller mapea a HTTP/AccountResponse
 * sin repetir ownership ni relectura de saldo.
 */
public final class DepositResult {

    public enum Kind {
        OK,
        MONTO_NEGATIVO,
        CUENTA_NO_EXISTE,
        NO_ES_PROPIETARIO,
        UPDATE_FALLIDO
    }

    private final Kind kind;
    private final double balance;

    private DepositResult(Kind kind, double balance) {
        this.kind = kind;
        this.balance = balance;
    }

    public static DepositResult ok(double newBalance) {
        return new DepositResult(Kind.OK, newBalance);
    }

    public static DepositResult montoNegativo(double amount) {
        return new DepositResult(Kind.MONTO_NEGATIVO, amount);
    }

    public static DepositResult cuentaNoExiste() {
        return new DepositResult(Kind.CUENTA_NO_EXISTE, 0);
    }

    public static DepositResult noEsPropietario() {
        return new DepositResult(Kind.NO_ES_PROPIETARIO, 0);
    }

    public static DepositResult updateFallido() {
        return new DepositResult(Kind.UPDATE_FALLIDO, 0);
    }

    public Kind getKind() {
        return kind;
    }

    public double getBalance() {
        return balance;
    }
}
