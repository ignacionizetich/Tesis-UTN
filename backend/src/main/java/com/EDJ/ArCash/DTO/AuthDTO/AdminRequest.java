package com.EDJ.ArCash.DTO.AuthDTO;

import com.EDJ.ArCash.validation.Dni;
import com.EDJ.ArCash.validation.PasswordNotSimilarToIdentity;
import com.EDJ.ArCash.validation.PersonName;
import com.EDJ.ArCash.validation.StrongPassword;
import com.EDJ.ArCash.validation.Username;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Alta de una cuenta administradora.
 *
 * <p>Se le exige la misma politica de contrasena que al registro publico: una cuenta con
 * permisos elevados no puede tener requisitos mas laxos que una cuenta comun.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@PasswordNotSimilarToIdentity(
        identityFields = {"username", "email", "dni", "name", "lastName"}
)
@Schema(description = "Petición para crear un usuario administrador")
public class AdminRequest {

    @Schema(description = "ID del usuario", example = "1")
    private long id;

    @NotBlank(message = "El nombre es obligatorio")
    @PersonName
    @Schema(description = "Nombre del administrador", example = "Ana")
    private String name;

    @NotBlank(message = "El apellido es obligatorio")
    @PersonName
    @Schema(description = "Apellido del administrador", example = "García")
    private String lastName;

    @NotBlank(message = "El DNI es obligatorio")
    @Dni
    @Schema(description = "DNI del administrador", example = "87654321")
    private String dni;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 120, message = "El email no puede superar los 120 caracteres")
    @Schema(description = "Correo electrónico", example = "ana.garcia@email.com")
    private String email;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Username
    @Schema(description = "Nombre de usuario", example = "ana.garcia")
    private String username;

    @Schema(description = "ID de la cuenta asociada", example = "20")
    private Long idAccount;

    @Schema(description = "Indica si el usuario está habilitado", example = "true")
    private boolean enabled;

    @NotBlank(message = "La contraseña es obligatoria")
    @StrongPassword
    @Schema(description = "Contraseña del administrador", example = "Arcash2026$admin")
    private String password;
}
