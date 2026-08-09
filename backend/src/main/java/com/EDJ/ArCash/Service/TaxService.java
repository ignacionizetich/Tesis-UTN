package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.DTO.AuthDTO.TaxPesosResponse;
import com.EDJ.ArCash.DTO.AuthDTO.TaxUsdResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TaxService {

    private static final double ALICUOTA_IVA = 0.21;
    private static final double ALICUOTA_COMISION_CONVERSION = 0.03;
    private static final double PORCENTAJE_COMISION_CONVERSION = 3.0;

    private static final String MONEDA_PESOS = "ARS";
    private static final String MONEDA_DOLARES = "USD";

    private final CotizationUsdService cotizationUsdService;

    public TaxService(CotizationUsdService cotizationUsdService) {
        this.cotizationUsdService = cotizationUsdService;
    }

    public TaxPesosResponse calcularPesos(double monto) {
        double iva = calcularIva(monto);

        TaxPesosResponse respuesta = new TaxPesosResponse();
        respuesta.setMontoOriginal(monto);
        respuesta.setMoneda(MONEDA_PESOS);
        respuesta.setIVA(iva);
        respuesta.setTotalFinal(monto + iva);
        return respuesta;
    }

    /**
     * Recibe un monto en dolares y devuelve el calculo ya convertido a pesos,
     * aunque la respuesta quede etiquetada como USD: el dashboard lee
     * montoOriginal como importe en ARS.
     */
    public TaxUsdResponse calcularUSD(double monto) {
        double tipoCambio = cotizationUsdService.obtenerCotizacionVenta();
        double montoEnPesos = monto * tipoCambio;
        double iva = calcularIva(montoEnPesos);

        TaxUsdResponse respuesta = new TaxUsdResponse();
        respuesta.setMontoOriginal(montoEnPesos);
        respuesta.setMoneda(MONEDA_DOLARES);
        respuesta.setPrecioDolar(tipoCambio);
        respuesta.setIVA(iva);
        respuesta.setTotalFinal(montoEnPesos + iva);
        return respuesta;
    }

    private double calcularIva(double montoBase) {
        return montoBase * ALICUOTA_IVA;
    }

    /**
     * Calcula la comision del 3% por conversion de ARS a USD.
     * TransactionService lee estas claves por nombre.
     */
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
