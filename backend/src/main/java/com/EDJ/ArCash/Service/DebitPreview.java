package com.EDJ.ArCash.Service;

/**
 * Vista previa del debito ARS (base + comision) sin consultar cotizacion.
 */
public record DebitPreview(
        double amountArs,
        double taxAmount,
        double taxPercentage,
        double totalDebitado
) {
}
