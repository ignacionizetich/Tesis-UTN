package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Access token renovado a partir de la cookie de refresh.
 *
 * <p>Solo lleva el access token: el refresh viaja en una cookie HttpOnly y nunca en el cuerpo,
 * para que no quede accesible desde JavaScript.
 */
@Schema(description = "Access token renovado")
public record RefreshTokenResponse(
        @Schema(description = "JWT de acceso de vida corta")
        String accessToken
) {
}
