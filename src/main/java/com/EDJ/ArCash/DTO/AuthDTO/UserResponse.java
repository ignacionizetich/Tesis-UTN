package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Schema(description = "Respuesta con información de usuario")
public class UserResponse {
    @Schema(description = "ID del usuario", example = "1")
    private long id;

    @Schema(description = "Nombre del usuario", example = "Juan")
    private String name;

    @Schema(description = "Apellido del usuario", example = "Pérez")
    private String lastName;

    @Schema(description = "DNI del usuario", example = "12345678")
    private String dni;

    @Schema(description = "Correo electrónico", example = "juan.perez@email.com")
    private String email;

    @Schema(description = "Nombre de usuario", example = "juanp")
    private String username;

    @Schema(description = "ID de la cuenta asociada", example = "10")
    private Long idAccount;

    @Schema(description = "Indica si el usuario está habilitado", example = "true")
    private boolean enabled;

    @Schema(description = "Indica si el usuario está activo", example = "true")
    private boolean active;
}