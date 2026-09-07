package com.EDJ.ArCash.DTO.AuthDTO;

import com.EDJ.ArCash.validation.MoneyAmount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para realizar una transferencia entre cuentas")
public class TranscationRequest {

    @NotNull(message = "El monto a transferir es obligatorio")
    @MoneyAmount
    @Schema(description = "Monto a transferir", example = "5000.00")
    private Double balance;
}
