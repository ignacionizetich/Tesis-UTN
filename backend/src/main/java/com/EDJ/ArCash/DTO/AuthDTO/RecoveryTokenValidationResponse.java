package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Estado de un enlace de recuperacion de contrasena.
 *
 * <p>Usa {@code valid} en lugar de {@code success} porque un token invalido no es un fallo de
 * la operacion: la consulta se resolvio bien y la respuesta es "no sirve".
 */
@Schema(description = "Validez de un enlace de recuperación de contraseña")
public record RecoveryTokenValidationResponse(
        @Schema(description = "true si el enlace se puede usar", example = "true")
        boolean valid,

        @Schema(description = "Motivo para mostrar al usuario", example = "Enlace de recuperación válido")
        String message
) {
}
