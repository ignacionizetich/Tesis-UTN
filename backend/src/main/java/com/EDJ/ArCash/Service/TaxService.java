package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.DTO.AuthDTO.TaxPesosResponse;
import com.EDJ.ArCash.DTO.AuthDTO.TaxUsdResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaxService {

    @Autowired
    private CotizationUsdService cotizationUsdService;

    public TaxPesosResponse calcularPesos(double monto){
        return calcularARS(monto, "ARS");
    }


    public TaxUsdResponse calcularUSD(double monto){
        double tipoCambio = cotizationUsdService.obtenerCotizacionVenta();
        double montoEnPesos = monto * tipoCambio;
        return calcularUSD(montoEnPesos, "USD");
    }



    private TaxPesosResponse calcularARS(double montoBase, String moneda){
         double iva = montoBase * 0.21;

         double total = montoBase + iva;

         TaxPesosResponse taxPesosResponse = new TaxPesosResponse();
         taxPesosResponse.setMontoOriginal(montoBase);
         taxPesosResponse.setMoneda(moneda);
         taxPesosResponse.setIVA(iva);
         taxPesosResponse.setTotalFinal(total);
         return taxPesosResponse;

    }

    private TaxUsdResponse calcularUSD(double montoBase, String moneda){
        double iva = montoBase * 0.21;



        double total = montoBase + iva ;

        TaxUsdResponse taxUsdResponse = new TaxUsdResponse();
        taxUsdResponse.setMontoOriginal(montoBase);
        taxUsdResponse.setMoneda(moneda);
        taxUsdResponse.setPrecioDolar(cotizationUsdService.obtenerCotizacionVenta());
        taxUsdResponse.setIVA(iva);
        taxUsdResponse.setTotalFinal(total);

        return taxUsdResponse;
    }
    
    /**
     * Calcula la comisión para conversión de ARS a USD
     * Aplica 3% de comisión por conversión
     * @param montoArs Monto en pesos argentinos
     * @return Map con información de comisión
     */
    public java.util.Map<String, Double> calcularImpuestosConversion(double montoArs) {
        // Comisión por conversión: 3%
        double comision = montoArs * 0.03;
        
        // Porcentaje total
        double porcentajeTotal = 3.0;
        
        java.util.Map<String, Double> resultado = new java.util.HashMap<>();
        resultado.put("impuestoPais", 0.0);
        resultado.put("percepcion", 0.0);
        resultado.put("totalImpuestos", comision);
        resultado.put("porcentajeTotal", porcentajeTotal);
        resultado.put("montoConImpuestos", montoArs + comision);
        
        return resultado;
    }
}
