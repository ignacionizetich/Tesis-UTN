package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Schema(description = "Respuesta al intentar cerrar sesión (logout)")
public class LogoutResponse {
    @Schema(description = "Indica si el logout fue exitoso", example = "true")
    private boolean success;

    @Schema(description = "Mensaje de respuesta", example = "Sesión cerrada correctamente")
    private String message;
}