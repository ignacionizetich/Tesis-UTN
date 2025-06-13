package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Schema(description = "Petición para crear un usuario administrador")
public class AdminRequest {
    @Schema(description = "ID del usuario", example = "1")
    private long id;

    @Schema(description = "Nombre del administrador", example = "Ana")
    private String name;

    @Schema(description = "Apellido del administrador", example = "García")
    private String lastName;

    @Schema(description = "DNI del administrador", example = "87654321")
    private String dni;

    @Schema(description = "Correo electrónico", example = "ana.garcia@email.com")
    private String email;

    @Schema(description = "Nombre de usuario", example = "anag")
    private String username;

    @Schema(description = "ID de la cuenta asociada", example = "20")
    private Long idAccount;

    @Schema(description = "Indica si el usuario está habilitado", example = "true")
    private boolean enabled;

    @Schema(description = "Contraseña del administrador", example = "Password123!")
    private String password;
}