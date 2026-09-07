package com.EDJ.ArCash.DTO.AuthDTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cuenta en dolares recien abierta.
 *
 * <p>Devuelve el id y el alias asignados para que el frontend pueda seleccionarla sin volver a
 * pedir la lista de cuentas. Cuando la apertura falla esos campos no existen, y
 * {@code NON_NULL} los omite en lugar de mandar nulos que el cliente tendria que filtrar.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resultado de la apertura de una cuenta en dólares")
public record OpenUsdAccountResponse(
        @Schema(description = "true si la cuenta quedó abierta", example = "true")
        boolean success,

        @Schema(description = "Mensaje para mostrar al usuario")
        String message,

        @Schema(description = "ID de la cuenta creada", example = "42")
        Long accountId,

        @Schema(description = "Alias asignado a la cuenta", example = "ana.gomez.usd")
        String accountAlias,

        @Schema(description = "Moneda de la cuenta", example = "USD")
        String currency
) {
}
