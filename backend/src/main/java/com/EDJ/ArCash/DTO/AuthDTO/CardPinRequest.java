package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Crear o cambiar PIN de tarjetas")
public class CardPinRequest {
    @Schema(description = "PIN de 6 dígitos")
    private String pin;

    @Schema(description = "Confirmación del PIN")
    private String confirmPin;

    @Schema(description = "PIN actual (solo al cambiar)")
    private String currentPin;
}
