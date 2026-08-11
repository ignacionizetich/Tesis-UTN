package com.EDJ.ArCash.Service;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Calcula montos de compra ARS→USD compartidos por transfer-con-conversion y buyUsd.
 * No valida saldos ni persiste: solo la formula.
 * {@link #previewDebit} no toca cotizacion; {@link #calculate} si (llamar solo si el saldo alcanza).
 */
@Service
public class ArsToUsdConversionService {

    private final TaxService taxService;
    private final CotizationUsdService cotizationUsdService;

    public ArsToUsdConversionService(TaxService taxService, CotizationUsdService cotizationUsdService) {
        this.taxService = taxService;
        this.cotizationUsdService = cotizationUsdService;
    }

    /**
     * Solo comision / total a debitar. No consulta la cotizacion externa/cache.
     */
    public DebitPreview previewDebit(double amountArs) {
        Map<String, Double> impuestos = taxService.calcularImpuestosConversion(amountArs);
        return new DebitPreview(
                amountArs,
                impuestos.get("totalImpuestos"),
                impuestos.get("porcentajeTotal"),
                impuestos.get("montoConImpuestos")
        );
    }

    /**
     * Tax + tasa venta + amountUsd (solo el monto base / rate).
     * Invocar despues de validar que el saldo cubre {@link DebitPreview#totalDebitado()}.
     */
    public ArsToUsdConversion calculate(double amountArs) {
        DebitPreview preview = previewDebit(amountArs);

        double exchangeRate = cotizationUsdService.obtenerCotizacionVenta();
        // Solo el monto base: la comision no se convierte a USD.
        double amountUsd = amountArs / exchangeRate;

        return new ArsToUsdConversion(
                preview.amountArs(),
                preview.taxAmount(),
                preview.taxPercentage(),
                preview.totalDebitado(),
                exchangeRate,
                amountUsd
        );
    }
}
