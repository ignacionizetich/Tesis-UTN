package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.DTO.AuthDTO.TaxPesosResponse;
import com.EDJ.ArCash.DTO.AuthDTO.TaxUsdResponse;

import java.util.Map;

/**
 * Calculo de impuestos con validacion de monto (antes en TaxController).
 */
public final class TaxCalculationResult {

    public enum Kind {
        OK_ARS,
        OK_USD,
        INVALID
    }

    private final Kind kind;
    private final String error;
    private final TaxPesosResponse ars;
    private final TaxUsdResponse usd;

    private TaxCalculationResult(Kind kind, String error, TaxPesosResponse ars, TaxUsdResponse usd) {
        this.kind = kind;
        this.error = error;
        this.ars = ars;
        this.usd = usd;
    }

    public static TaxCalculationResult okArs(TaxPesosResponse response) {
        return new TaxCalculationResult(Kind.OK_ARS, null, response, null);
    }

    public static TaxCalculationResult okUsd(TaxUsdResponse response) {
        return new TaxCalculationResult(Kind.OK_USD, null, null, response);
    }

    public static TaxCalculationResult invalid(String error) {
        return new TaxCalculationResult(Kind.INVALID, error, null, null);
    }

    public Kind getKind() {
        return kind;
    }

    public TaxPesosResponse getArs() {
        return ars;
    }

    public TaxUsdResponse getUsd() {
        return usd;
    }

    public Map<String, String> toErrorBody() {
        return Map.of("error", error);
    }
}
