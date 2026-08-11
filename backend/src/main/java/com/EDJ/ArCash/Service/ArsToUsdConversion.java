package com.EDJ.ArCash.Service;

/**
 * Resultado del calculo ARS→USD (comision 3% + cotizacion venta).
 * amountUsd se obtiene solo del monto base; la comision no se convierte.
 */
public record ArsToUsdConversion(
        double amountArs,
        double taxAmount,
        double taxPercentage,
        double totalDebitado,
        double exchangeRate,
        double amountUsd
) {
}
