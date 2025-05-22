package com.EDJ.ArCash.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaxPesosRequest {

    @NotNull(message = "el monto en pesos es obligatorio")
    @Min(value = 0, message = "El monto debe ser mayor que cero")
    private double montoARS;
}
