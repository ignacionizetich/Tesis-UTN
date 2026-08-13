package com.EDJ.ArCash.Service.result;

import java.util.LinkedHashMap;
import java.util.Map;

import com.EDJ.ArCash.DTO.AuthDTO.BuyUsdResponse;

public final class BuyUsdResult {

    private final boolean success;
    private final String message;
    private final Double amountArs;
    private final Double amountUsd;
    private final Double exchangeRate;
    private final Double taxAmount;
    private final Double taxPercentage;
    private final Double totalDebitado;
    private final Double newBalanceArs;
    private final Double newBalanceUsd;

    private BuyUsdResult(boolean success, String message,
                         Double amountArs, Double amountUsd, Double exchangeRate,
                         Double taxAmount, Double taxPercentage, Double totalDebitado,
                         Double newBalanceArs, Double newBalanceUsd) {
        this.success = success;
        this.message = message;
        this.amountArs = amountArs;
        this.amountUsd = amountUsd;
        this.exchangeRate = exchangeRate;
        this.taxAmount = taxAmount;
        this.taxPercentage = taxPercentage;
        this.totalDebitado = totalDebitado;
        this.newBalanceArs = newBalanceArs;
        this.newBalanceUsd = newBalanceUsd;
    }

    public static BuyUsdResult fail(String message) {
        return new BuyUsdResult(false, message, null, null, null, null, null, null, null, null);
    }

    public static BuyUsdResult ok(String message,
                                  double amountArs,
                                  double amountUsd,
                                  double exchangeRate,
                                  double taxAmount,
                                  double taxPercentage,
                                  double totalDebitado,
                                  double newBalanceArs,
                                  double newBalanceUsd) {
        return new BuyUsdResult(true, message, amountArs, amountUsd, exchangeRate,
                taxAmount, taxPercentage, totalDebitado, newBalanceArs, newBalanceUsd);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Double getAmountArs() {
        return amountArs;
    }

    public Double getAmountUsd() {
        return amountUsd;
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

    public BuyUsdResponse toResponse() {
        return new BuyUsdResponse(
                true,
                message,
                amountArs,
                amountUsd,
                exchangeRate,
                taxAmount,
                taxPercentage,
                totalDebitado,
                newBalanceArs,
                newBalanceUsd
        );
    }

    /** Cuerpo de error HTTP actual: solo success + message. */
    public Map<String, Object> toErrorMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", false);
        map.put("message", message);
        return map;
    }
}
