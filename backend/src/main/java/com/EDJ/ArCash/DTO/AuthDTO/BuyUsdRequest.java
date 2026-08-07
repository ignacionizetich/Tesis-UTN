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
@Schema(description = "Solicitud para comprar dólares desde cuenta en pesos")
public class BuyUsdRequest {
    
    @NotNull(message = "El monto en pesos es obligatorio")
    @Min(value = 1, message = "El monto debe ser mayor a 0")
    @Schema(description = "Monto en pesos a convertir a dólares", example = "10000.00", required = true)
    private double amountArs;
}
