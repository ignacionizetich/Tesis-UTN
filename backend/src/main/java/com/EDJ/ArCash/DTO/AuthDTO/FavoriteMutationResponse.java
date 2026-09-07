package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado de un alta, baja o modificacion de favorito.
 *
 * <p>El campo {@code status} se mantiene como texto ("SUCCESS"/"ERROR") porque el frontend
 * decide con {@code response.body?.status === 'SUCCESS'}; cambiarlo a booleano rompe el cliente.
 */
@Schema(description = "Resultado de una operación sobre contactos favoritos")
public record FavoriteMutationResponse(
        @Schema(description = "SUCCESS o ERROR", example = "SUCCESS")
        String status,

        @Schema(description = "Detalle para mostrar al usuario",
                example = "Contacto agregado a favoritos correctamente")
        String message
) {

    private static final String SUCCESS = "SUCCESS";
    private static final String ERROR = "ERROR";

    public static FavoriteMutationResponse success(String message) {
        return new FavoriteMutationResponse(SUCCESS, message);
    }

    public static FavoriteMutationResponse error(String message) {
        return new FavoriteMutationResponse(ERROR, message);
    }
}
