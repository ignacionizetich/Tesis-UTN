package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardPinVerifyRequest {
    @Schema(description = "PIN de 6 dígitos")
    private String pin;
}
