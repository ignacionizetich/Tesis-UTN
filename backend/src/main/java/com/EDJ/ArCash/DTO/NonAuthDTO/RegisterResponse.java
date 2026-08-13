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

    @Schema(description = "Mensaje de la operación de registro", example = "Usuario registrado correctamente")
    private String message;

    @Schema(description = "Indica si el registro fue exitoso", example = "true")
    private boolean success;

    public RegisterResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
