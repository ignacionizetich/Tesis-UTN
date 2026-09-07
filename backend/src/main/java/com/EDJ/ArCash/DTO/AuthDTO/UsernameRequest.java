package com.EDJ.ArCash.DTO.AuthDTO;

import com.EDJ.ArCash.validation.Username;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para cambiar el nombre de usuario")
public class UsernameRequest {

    @NotBlank(message = "El nuevo nombre de usuario es obligatorio")
    @Username
    @Schema(description = "Nuevo nombre de usuario", example = "juan.perez.02")
    private String newUsername;
}
