package com.EDJ.ArCash.DTO.AuthDTO;

import com.EDJ.ArCash.validation.ValidCardPin;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * Alta o cambio del PIN de tarjetas.
 *
 * <p>{@code pin} pasa por la politica de PIN debil, pero {@code currentPin} solo se valida como
 * formato: es el PIN que el usuario ya tiene y podria haberse creado antes de la politica.
 * Aplicarle {@link ValidCardPin} dejaria a esos usuarios sin forma de cambiarlo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Crear o cambiar PIN de tarjetas")
public class CardPinRequest {

    @NotBlank(message = "El PIN es obligatorio")
    @ValidCardPin
    @Schema(description = "PIN de 6 dígitos")
    private String pin;

    @NotBlank(message = "La confirmación del PIN es obligatoria")
    @Schema(description = "Confirmación del PIN")
    private String confirmPin;

    @Pattern(regexp = "^$|^\\d{6}$", message = "El PIN actual debe tener 6 dígitos")
    @Schema(description = "PIN actual (solo al cambiar)")
    private String currentPin;
}
