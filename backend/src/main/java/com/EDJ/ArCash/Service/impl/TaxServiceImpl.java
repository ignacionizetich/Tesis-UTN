package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.TransactionService;
import com.EDJ.ArCash.Service.interfaces.CotizationUsdService;
import com.EDJ.ArCash.Service.interfaces.TaxService;
import com.EDJ.ArCash.Service.result.*;

import com.EDJ.ArCash.DTO.AuthDTO.TaxPesosResponse;
import com.EDJ.ArCash.DTO.AuthDTO.TaxUsdResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaxServiceImpl implements TaxService {

    private static final double ALICUOTA_IVA = 0.21;
    private static final double PORCENTAJE_IVA = 21.0;
    private static final double ALICUOTA_COMISION_CONVERSION = 0.03;
    private static final double PORCENTAJE_COMISION_CONVERSION = 3.0;

    private static final String MONEDA_PESOS = "ARS";
    private static final String MONEDA_DOLARES = "USD";

    private final CotizationUsdService cotizationUsdService;


    public TaxPesosResponse calcularPesos(double monto) {
        double iva = calcularIva(monto);

        TaxPesosResponse respuesta = new TaxPesosResponse();
        respuesta.setMontoOriginal(monto);
        respuesta.setMoneda(MONEDA_PESOS);
        respuesta.setAlicuotaIva(PORCENTAJE_IVA);
        respuesta.setIVA(iva);
        respuesta.setTotalFinal(monto + iva);
        return respuesta;
    }

    public TaxCalculationResult calcularPesosRequest(double montoARS) {
        if (montoARS <= 0) {
            return TaxCalculationResult.invalid("El monto en ARS no puede ser cero o negativo.");
        }
        return TaxCalculationResult.okArs(calcularPesos(montoARS));
    }

    public TaxUsdResponse calcularUSD(double monto) {
        double tipoCambioVenta = cotizationUsdService.obtenerCotizacionVenta();
        double tipoCambioCompra = 0;
        try {
            tipoCambioCompra = cotizationUsdService.obtenerCotizacionCompra();
        } catch (Exception ignored) {
            // La venta alcanza para el cálculo; la compra es informativa.
        }

        double montoEnPesos = monto * tipoCambioVenta;
        double iva = calcularIva(montoEnPesos);

        TaxUsdResponse respuesta = new TaxUsdResponse();
        respuesta.setMontoUsd(monto);
        respuesta.setMontoOriginal(montoEnPesos);
        respuesta.setMoneda(MONEDA_DOLARES);
        respuesta.setPrecioDolar(tipoCambioVenta);
        respuesta.setDolarVenta(tipoCambioVenta);
        respuesta.setDolarCompra(tipoCambioCompra);
        respuesta.setNombreCotizacion(cotizationUsdService.obtenerNombreCotizacion());
        respuesta.setCasa(cotizationUsdService.obtenerCasaCotizacion());
        respuesta.setFechaActualizacion(cotizationUsdService.obtenerFechaActualizacion());
        respuesta.setAlicuotaIva(PORCENTAJE_IVA);
        respuesta.setIVA(iva);
        respuesta.setTotalFinal(montoEnPesos + iva);
        return respuesta;
    }

    public TaxCalculationResult calcularUsdRequest(double montoUSD) {
        if (montoUSD <= 0) {
            return TaxCalculationResult.invalid("El monto en USD no puede ser cero.");
        }
        return TaxCalculationResult.okUsd(calcularUSD(montoUSD));
    }

    private double calcularIva(double montoBase) {
        return montoBase * ALICUOTA_IVA;
    }

    public Map<String, Double> calcularImpuestosConversion(double montoArs) {
        double comision = montoArs * ALICUOTA_COMISION_CONVERSION;

        Map<String, Double> resultado = new HashMap<>();
        resultado.put("impuestoPais", 0.0);
        resultado.put("percepcion", 0.0);
        resultado.put("totalImpuestos", comision);
        resultado.put("porcentajeTotal", PORCENTAJE_COMISION_CONVERSION);
        resultado.put("montoConImpuestos", montoArs + comision);

        return resultado;
    }
}
