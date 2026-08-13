package com.EDJ.ArCash.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resultado de sellUsd. El controller mapea el exito a SellUsdResponse;
 * el fail se expone como map minimo {success, message}.
 */
public final class SellUsdResult {

    private final boolean success;
    private final String message;
    private final Double amountUsd;
    private final Double amountArs;
    private final Double exchangeRate;
    private final Double taxAmount;
    private final Double taxPercentage;
    private final Double totalDebitado;
    private final Double newBalanceArs;
    private final Double newBalanceUsd;

    private SellUsdResult(boolean success, String message,
                          Double amountUsd, Double amountArs, Double exchangeRate,
                          Double taxAmount, Double taxPercentage, Double totalDebitado,
                          Double newBalanceArs, Double newBalanceUsd) {
        this.success = success;
        this.message = message;
        this.amountUsd = amountUsd;
        this.amountArs = amountArs;
        this.exchangeRate = exchangeRate;
        this.taxAmount = taxAmount;
        this.taxPercentage = taxPercentage;
        this.totalDebitado = totalDebitado;
        this.newBalanceArs = newBalanceArs;
        this.newBalanceUsd = newBalanceUsd;
    }

    public static SellUsdResult fail(String message) {
        return new SellUsdResult(false, message, null, null, null, null, null, null, null, null);
    }

    public static SellUsdResult ok(String message,
                                   double amountUsd,
                                   double amountArs,
                                   double exchangeRate,
                                   double taxAmount,
                                   double taxPercentage,
                                   double totalDebitado,
                                   double newBalanceArs,
                                   double newBalanceUsd) {
        return new SellUsdResult(true, message, amountUsd, amountArs, exchangeRate,
                taxAmount, taxPercentage, totalDebitado, newBalanceArs, newBalanceUsd);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Double getAmountUsd() {
        return amountUsd;
    }

    public Double getAmountArs() {
        return amountArs;
    }

    public Double getExchangeRate() {
        return exchangeRate;
    }

    public Double getTaxAmount() {
        return taxAmount;
    }

    public Double getTaxPercentage() {
        return taxPercentage;
    }

    public Double getTotalDebitado() {
        return totalDebitado;
    }

    public Double getNewBalanceArs() {
        return newBalanceArs;
    }

    public Double getNewBalanceUsd() {
        return newBalanceUsd;
    }

    public Map<String, Object> toErrorMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", false);
        map.put("message", message);
        return map;
    }
}
