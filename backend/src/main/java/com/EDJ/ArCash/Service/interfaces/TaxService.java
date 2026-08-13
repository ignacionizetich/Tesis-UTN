package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.DTO.AuthDTO.TaxPesosResponse;
import com.EDJ.ArCash.DTO.AuthDTO.TaxUsdResponse;
import com.EDJ.ArCash.Service.result.TaxCalculationResult;
import java.util.Map;

public interface TaxService {
    public TaxPesosResponse calcularPesos(double monto);

    public TaxCalculationResult calcularPesosRequest(double montoARS);

    public TaxUsdResponse calcularUSD(double monto);

    public TaxCalculationResult calcularUsdRequest(double montoUSD);

    public Map<String, Double> calcularImpuestosConversion(double montoArs);

}
