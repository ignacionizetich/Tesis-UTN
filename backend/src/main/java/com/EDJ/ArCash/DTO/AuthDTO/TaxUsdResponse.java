package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Respuesta con el cálculo de impuestos sobre un monto en dólares")
public class TaxUsdResponse {
    @Schema(description = "Monto original en dólares", example = "100.00")
    private double montoOriginal;

    @Schema(description = "Moneda utilizada", example = "USD")
    private String moneda;

    @Schema(description = "Precio del dólar utilizado para el cálculo", example = "950.00")
    private double precioDolar;

    @Schema(description = "IVA calculado", example = "21.00")
    private double IVA;


    @Schema(description = "Total final con impuestos", example = "116.00")
    private double totalFinal;
}