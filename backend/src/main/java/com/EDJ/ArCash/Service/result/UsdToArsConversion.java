package com.EDJ.ArCash.Service.result;

public record UsdToArsConversion(
        double amountUsd,
        double taxAmount,
        double taxPercentage,
        double totalDebitado,
        double exchangeRate,
        double amountArs
) {
}
