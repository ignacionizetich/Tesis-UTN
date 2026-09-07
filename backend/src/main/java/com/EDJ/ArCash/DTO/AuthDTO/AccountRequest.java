package com.EDJ.ArCash.DTO.AuthDTO;

import com.EDJ.ArCash.validation.MoneyAmount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Monto a ingresar en una cuenta.
 *
 * <p>El campo es {@code Double} y no {@code double}: sobre un primitivo, {@code @NotNull} nunca
 * falla y un cuerpo JSON sin la clave {@code balance} llegaria al servicio como un deposito de
 * cero en lugar de rechazarse como peticion incompleta.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Schema(description = "Solicitud para ingresar dinero en una cuenta")
public class AccountRequest {

    @NotNull(message = "El monto es obligatorio")
    @MoneyAmount
    @Schema(description = "Monto a ingresar", example = "1000.50")
    private Double balance;
}
