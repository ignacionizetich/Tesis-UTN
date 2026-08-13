package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Solicitud para vender dólares a cuenta en pesos")
public class SellUsdRequest {

    @NotNull(message = "El monto en dólares es obligatorio")
    @Min(value = 1, message = "El monto debe ser mayor a 0")
    @Schema(description = "Monto en dólares a convertir a pesos", example = "100.00", required = true)
    private double amountUsd;
}
