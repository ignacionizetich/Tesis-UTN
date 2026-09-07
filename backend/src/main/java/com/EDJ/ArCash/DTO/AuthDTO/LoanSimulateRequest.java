package com.EDJ.ArCash.DTO.AuthDTO;

import com.EDJ.ArCash.validation.MoneyAmount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Parametros de una simulacion o alta de prestamo.
 *
 * <p>Los rangos de negocio concretos (monto minimo y plazos habilitados) los sigue validando
 * {@code LoanService}, que es donde viven las reglas del producto. Aca solo se acotan los
 * valores estructuralmente imposibles, para que nunca lleguen al calculo de amortizacion.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Solicitud de simulación o alta de préstamo")
public class LoanSimulateRequest {

    @NotNull(message = "El capital solicitado es obligatorio")
    @MoneyAmount
    @Schema(description = "Capital solicitado en pesos", example = "100000.00")
    private Double principal;

    @NotNull(message = "La cantidad de cuotas es obligatoria")
    @Min(value = 1, message = "El préstamo debe tener al menos 1 cuota")
    @Max(value = 120, message = "El préstamo no puede superar las 120 cuotas")
    @Schema(description = "Cantidad de cuotas", example = "12")
    private Integer installments;
}
