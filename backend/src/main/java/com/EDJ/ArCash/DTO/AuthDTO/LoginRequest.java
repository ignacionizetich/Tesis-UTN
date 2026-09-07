package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Credenciales de inicio de sesion.
 *
 * <p>A diferencia del registro, aca no se aplica la politica de contrasena fuerte: el login
 * solo verifica lo que el usuario ya tiene guardado, y validar fortaleza al entrar bloquearia
 * a las cuentas creadas antes de que la politica existiera. Solo se comprueba presencia y una
 * cota de longitud, para no enviar a BCrypt entradas arbitrariamente grandes.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos de la solicitud de inicio de sesión")
public class LoginRequest {

    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 120, message = "El usuario no puede superar los 120 caracteres")
    @Schema(description = "Nombre de usuario o email", example = "juan.perez.01")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 200, message = "La contraseña no puede superar los 200 caracteres")
    @Schema(description = "Contraseña del usuario", example = "Arcash2026$seguro")
    private String password;

    @Schema(description = "Campo de habilitación (opcional)", example = "true")
    private String enable;
}
