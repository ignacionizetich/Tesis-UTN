package com.EDJ.ArCash.Service.result;

import com.EDJ.ArCash.DTO.AuthDTO.AccountResponse;

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
    private final String message;

    private DepositResult(Kind kind, double balance, String message) {
        this.kind = kind;
        this.balance = balance;
        this.message = message;
    }

    public static DepositResult ok(double newBalance) {
        return new DepositResult(Kind.OK, newBalance, "Ingreso de dinero realizado correctamente.");
    }

    public static DepositResult montoNegativo(double amount) {
        return new DepositResult(Kind.MONTO_NEGATIVO, amount, "El monto a ingresar no puede ser negativo.");
    }

    public static DepositResult cuentaNoExiste() {
        return new DepositResult(Kind.CUENTA_NO_EXISTE, 0, "La cuenta no existe.");
    }

    public static DepositResult noEsPropietario() {
        return new DepositResult(Kind.NO_ES_PROPIETARIO, 0,
                "El usuario no es propietario legitimo de la cuenta");
    }

    public static DepositResult updateFallido() {
        return new DepositResult(Kind.UPDATE_FALLIDO, 0,
                "No se pudo actualizar el balance. Verifique el ID ingresado.");
    }

    public Kind getKind() {
        return kind;
    }

    public double getBalance() {
        return balance;
    }

    public String getMessage() {
        return message;
    }

    public AccountResponse toAccountResponse() {
        return new AccountResponse(kind == Kind.OK, message, balance);
    }
}
