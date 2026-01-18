
package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Schema(description = "Respuesta al cambiar el alias de la cuenta")
public class AliasResponse {
    @Schema(description = "Indica si el cambio fue exitoso", example = "true")
    private boolean success;

    @Schema(description = "Mensaje de respuesta", example = "Alias cambiado correctamente")
    private String message;
}