package com.EDJ.ArCash.DTO;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TaxUsdResponse {
    private double montoOriginal;
    private String moneda;
    private double precioDolar;
    private double IVA;
    private double percepcionGanancias;
    private double totalFinal;

}
