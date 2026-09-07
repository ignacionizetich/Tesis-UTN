package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.TaxService;
import com.EDJ.ArCash.Service.interfaces.CotizationUsdService;
import com.EDJ.ArCash.Service.interfaces.UsdToArsConversionService;
import com.EDJ.ArCash.Service.result.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UsdToArsConversionServiceImpl implements UsdToArsConversionService {

    private final TaxService taxService;
    private final CotizationUsdService cotizationUsdService;



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
