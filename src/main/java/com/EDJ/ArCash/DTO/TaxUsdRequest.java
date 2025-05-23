package com.EDJ.ArCash.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaxUsdRequest {

    @NotNull(message = "el monto en USD debe ser obligatorio")
    @Min(value = 0, message = "El monto debe ser mayor a 0")
    private double montoUSD;



}
