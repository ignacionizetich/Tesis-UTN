package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Listado de favoritos de un usuario.
 *
 * <p>Va envuelto en un objeto en lugar de devolver el array pelado para no romper al cliente,
 * que lee {@code response?.favorites}.
 */
@Schema(description = "Listado de contactos favoritos")
public record FavoriteListResponse(
        @Schema(description = "SUCCESS o ERROR", example = "SUCCESS")
        String status,

        @Schema(description = "Contactos favoritos del usuario")
        List<FavoriteContactResponse> favorites
) {

    public static FavoriteListResponse of(List<FavoriteContactResponse> favorites) {
        return new FavoriteListResponse("SUCCESS", favorites);
    }
}
