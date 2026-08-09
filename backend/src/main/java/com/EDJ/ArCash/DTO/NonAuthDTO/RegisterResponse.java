package com.EDJ.ArCash.DTO.NonAuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta al registrar un nuevo usuario")
public class RegisterResponse {
    /**
     * Fase 8: el resto de la API usa "message"; este DTO expone "mensaje".
     * El frontend admin ya contempla ambos; unificar el contrato en Fase 8.
     */
    @Schema(description = "Mensaje de la operación de registro", example = "Usuario registrado correctamente")
    private String mensaje;

    @Schema(description = "Indica si el registro fue exitoso", example = "true")
    private boolean success;

    public RegisterResponse(boolean success, String mensaje) {
        this.success = success;
        this.mensaje = mensaje;
    }
}