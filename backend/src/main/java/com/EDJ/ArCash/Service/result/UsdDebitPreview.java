package com.EDJ.ArCash.Service.result;

/**
 * Vista previa del debito USD (base + comision) sin consultar cotizacion.
 */
public record UsdDebitPreview(
        double amountUsd,
        double taxAmount,
        double taxPercentage,
        double totalDebitado
) {
}
