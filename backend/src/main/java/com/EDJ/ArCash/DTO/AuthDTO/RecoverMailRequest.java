package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Email al que enviar el enlace de recuperacion de contrasena.
 *
 * <p>Reemplaza al {@code Map<String, String>} que recibia el endpoint: con un mapa, un cuerpo
 * sin la clave {@code email} pasaba como {@code null} hasta el servicio, el contrato no
 * aparecia en el OpenAPI generado y Bean Validation no tenia donde engancharse.
 */
public record RecoverMailRequest(

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 120, message = "El email no puede superar los 120 caracteres")
        @Schema(description = "Email de la cuenta a recuperar", example = "juan.perez@email.com")
        String email
) {}
