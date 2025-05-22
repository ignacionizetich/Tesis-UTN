package com.EDJ.ArCash.DTO;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TaxPesosResponse {
    private double montoOriginal;
    private String moneda;
    private double IVA;
    private double totalFinal;

}
