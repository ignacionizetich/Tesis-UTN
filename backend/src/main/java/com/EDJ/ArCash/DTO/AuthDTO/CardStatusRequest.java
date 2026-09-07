package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Cambio de estado de una tarjeta")
public class CardStatusRequest {

    @NotBlank(message = "El estado es obligatorio")
    @Schema(description = "Nuevo estado de la tarjeta", allowableValues = {"ACTIVE", "PAUSED"})
    private String status;
}
