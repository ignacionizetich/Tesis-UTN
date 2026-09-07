package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Estado de la sesion asociada a un access token.
 *
 * <p>{@code status} es un codigo estable ({@code ACTIVE}, {@code INACTIVE}, {@code ERROR}) para
 * que el cliente ramifique sobre el, y {@code message} el texto que se muestra.
 */
@Schema(description = "Estado de la sesión del usuario")
public record SessionStatusResponse(
        @Schema(description = "ACTIVE, INACTIVE o ERROR", example = "ACTIVE")
        String status,

        @Schema(description = "Detalle legible del estado", example = "Sesión activa")
        String message
) {
}
