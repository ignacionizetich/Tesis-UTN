package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta al realizar una transacción")
public class TransactionResponse {
    @Schema(description = "Indica si la transacción fue exitosa", example = "true")
    private boolean success;

    @Schema(description = "Mensaje descriptivo del resultado de la transacción", example = "Transferencia realizada correctamente")
    private String message;
}