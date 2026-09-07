package com.EDJ.ArCash.DTO.NonAuthDTO;

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
 * Datos del registro publico.
 *
 * <p>La restriccion de clase {@link PasswordNotSimilarToIdentity} es la que impide que la
 * contrasena repita el nombre de usuario, el email, el DNI o el nombre real: son comprobaciones
 * cruzadas entre campos, imposibles de expresar con anotaciones campo por campo.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@PasswordNotSimilarToIdentity(
        identityFields = {"alias", "email", "dni", "name", "lastName"}
)
@Schema(description = "Solicitud para registrar un nuevo usuario")
public class RegistrerRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @PersonName
    @Schema(description = "Nombre del usuario", example = "Juan")
    private String name;

    @NotBlank(message = "El apellido es obligatorio")
    @PersonName
    @Schema(description = "Apellido del usuario", example = "Pérez")
    private String lastName;

    @NotBlank(message = "El DNI es obligatorio")
    @Dni
    @Schema(description = "DNI del usuario", example = "12345678")
    private String dni;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 120, message = "El email no puede superar los 120 caracteres")
    @Schema(description = "Correo electrónico del usuario", example = "juan.perez@email.com")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @StrongPassword
    @Schema(description = "Contraseña del usuario", example = "Arcash2026$seguro")
    private String password;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Username
    @Schema(description = "Nombre de usuario", example = "juan.perez.01")
    private String alias;
}
