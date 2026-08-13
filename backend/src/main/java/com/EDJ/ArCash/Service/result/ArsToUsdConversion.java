package com.EDJ.ArCash.Service.result;

public record ArsToUsdConversion(
        double amountArs,
        double taxAmount,
        double taxPercentage,
        double totalDebitado,
        double exchangeRate,
        double amountUsd
) {
}
