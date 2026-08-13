package com.EDJ.ArCash.Service;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Calcula montos de venta USD→ARS para sellUsd.
 * No valida saldos ni persiste: solo la formula.
 * {@link #previewDebit} no toca cotizacion; {@link #calculate} si (llamar solo si el saldo alcanza).
 */
@Service
public class UsdToArsConversionService {

    private final TaxService taxService;
    private final CotizationUsdService cotizationUsdService;

    public UsdToArsConversionService(TaxService taxService, CotizationUsdService cotizationUsdService) {
        this.taxService = taxService;
        this.cotizationUsdService = cotizationUsdService;
    }

    /**
     * Solo comision / total a debitar en USD. No consulta la cotizacion externa/cache.
     */
    public UsdDebitPreview previewDebit(double amountUsd) {
        Map<String, Double> impuestos = taxService.calcularImpuestosConversion(amountUsd);
        return new UsdDebitPreview(
                amountUsd,
                impuestos.get("totalImpuestos"),
                impuestos.get("porcentajeTotal"),
                impuestos.get("montoConImpuestos")
        );
    }

    /**
     * Tax + tasa compra + amountArs (solo el monto base * rate).
     * Invocar despues de validar que el saldo cubre {@link UsdDebitPreview#totalDebitado()}.
     */
    public UsdToArsConversion calculate(double amountUsd) {
        UsdDebitPreview preview = previewDebit(amountUsd);

        double exchangeRate = cotizationUsdService.obtenerCotizacionCompra();
        // Solo el monto base: la comision no se convierte a ARS.
        double amountArs = amountUsd * exchangeRate;

        return new UsdToArsConversion(
                preview.amountUsd(),
                preview.taxAmount(),
                preview.taxPercentage(),
                preview.totalDebitado(),
                exchangeRate,
                amountArs
        );
    }
}
