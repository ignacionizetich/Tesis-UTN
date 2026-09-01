package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.TaxService;
import com.EDJ.ArCash.Service.interfaces.CotizationUsdService;
import com.EDJ.ArCash.Service.interfaces.ArsToUsdConversionService;
import com.EDJ.ArCash.Service.result.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArsToUsdConversionServiceImpl implements ArsToUsdConversionService {

    private final TaxService taxService;
    private final CotizationUsdService cotizationUsdService;


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
