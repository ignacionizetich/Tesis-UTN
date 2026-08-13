package com.EDJ.ArCash.Service.result;

public final class TransferOperationResult {

    private final boolean success;
    private final String message;
    private final Double montoRequerido;
    private final Double saldoActual;
    private final Double impuestos;

    private TransferOperationResult(boolean success, String message,
                                    Double montoRequerido, Double saldoActual, Double impuestos) {
        this.success = success;
        this.message = message;
        this.montoRequerido = montoRequerido;
        this.saldoActual = saldoActual;
        this.impuestos = impuestos;
    }

    public static TransferOperationResult ok() {
        return new TransferOperationResult(true, null, null, null, null);
    }

    public static TransferOperationResult ok(String message) {
        return new TransferOperationResult(true, message, null, null, null);
    }

    public static TransferOperationResult fail(String message) {
        return new TransferOperationResult(false, message, null, null, null);
    }

    public static TransferOperationResult failInsufficient(String message,
                                                          double montoRequerido,
                                                          double saldoActual,
                                                          double impuestos) {
        return new TransferOperationResult(false, message, montoRequerido, saldoActual, impuestos);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Double getMontoRequerido() {
        return montoRequerido;
    }

    public Double getSaldoActual() {
        return saldoActual;
    }

    public Double getImpuestos() {
        return impuestos;
    }
}
