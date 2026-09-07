package com.EDJ.ArCash.DTO.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta generica de operaciones que solo informan si salieron bien y por que.
 *
 * <p>Reemplaza los {@code Map.of("success", ..., "message", ...)} que se repetian en varios
 * controllers: al ser un tipo unico, la forma queda documentada en OpenAPI y el compilador
 * impide que un endpoint escriba la clave mal.
 */
@Schema(description = "Resultado simple de una operación")
public record ApiMessageResponse(
        @Schema(description = "true si la operación se completó", example = "true")
        boolean success,

        @Schema(description = "Mensaje para mostrar al usuario",
                example = "Contraseña actualizada correctamente")
        String message
) {

    public static ApiMessageResponse success(String message) {
        return new ApiMessageResponse(true, message);
    }

    public static ApiMessageResponse failure(String message) {
        return new ApiMessageResponse(false, message);
    }
}
