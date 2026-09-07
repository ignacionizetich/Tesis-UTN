package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * PIN enviado para desbloquear los datos sensibles de una tarjeta.
 *
 * <p>Solo se valida el formato. Verificar aca la politica de PIN debil filtraria informacion:
 * un atacante sabria que cualquier PIN rechazado por "demasiado comun" no es el del usuario,
 * y podria descartar candidatos sin gastar intentos del contador de bloqueo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Verificación del PIN de tarjetas")
public class CardPinVerifyRequest {

    @NotBlank(message = "El PIN es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El PIN debe tener 6 dígitos")
    @Schema(description = "PIN de 6 dígitos")
    private String pin;
}
