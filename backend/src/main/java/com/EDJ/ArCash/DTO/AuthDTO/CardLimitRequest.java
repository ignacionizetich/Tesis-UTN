package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Nuevo limite diario de consumo de una tarjeta.
 *
 * <p>No usa {@code @MoneyAmount} porque cero es un valor legitimo: significa dejar la tarjeta
 * sin margen de consumo, que es distinto de pausarla.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Actualización del límite diario de una tarjeta")
public class CardLimitRequest {

    @NotNull(message = "El límite diario es obligatorio")
    @DecimalMin(value = "0.00", message = "El límite no puede ser negativo")
    @DecimalMax(value = "99999999.99", message = "El límite excede el máximo permitido")
    @Digits(integer = 8, fraction = 2, message = "El límite admite como máximo 2 decimales")
    @Schema(description = "Límite diario en la moneda de la tarjeta", example = "50000.00")
    private Double dailyLimit;
}
