package com.EDJ.ArCash.DTO.AuthDTO;

import com.EDJ.ArCash.validation.MoneyAmount;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @MoneyAmount
    @Schema(description = "Monto en dólares a convertir a pesos", example = "100.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Double amountUsd;
}
