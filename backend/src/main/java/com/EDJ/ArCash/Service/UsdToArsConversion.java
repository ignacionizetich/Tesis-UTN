package com.EDJ.ArCash.Service;

/**
 * Resultado del calculo USD→ARS (comision 3% + cotizacion compra).
 * amountArs se obtiene solo del monto base; la comision no se convierte.
 */
public record UsdToArsConversion(
        double amountUsd,
        double taxAmount,
        double taxPercentage,
        double totalDebitado,
        double exchangeRate,
        double amountArs
) {
}
