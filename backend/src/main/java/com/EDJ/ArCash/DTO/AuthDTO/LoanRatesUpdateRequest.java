package com.EDJ.ArCash.DTO.AuthDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Actualizacion de las tasas mensuales de prestamos por plazo.
 *
 * <p>La lista lleva {@code @Valid} para que la validacion baje a cada elemento: sin esa
 * anotacion, Bean Validation comprueba la lista pero ignora por completo las restricciones
 * declaradas dentro de {@code LoanRateUpdateItem}.
 */
@Getter
@Setter
@NoArgsConstructor
public class LoanRatesUpdateRequest {

    @NotEmpty(message = "Debés enviar al menos una tasa")
    @Valid
    private List<LoanRateUpdateItem> rates;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LoanRateUpdateItem {

        @NotNull(message = "La cantidad de cuotas es obligatoria")
        @Min(value = 1, message = "La cantidad de cuotas debe ser positiva")
        private Integer installments;

        /** Porcentaje mensual (ej. 4.0 = 4%). */
        @NotNull(message = "La tasa mensual es obligatoria")
        @DecimalMin(value = "0.0", message = "La tasa no puede ser negativa")
        @DecimalMax(value = "100.0", message = "La tasa mensual no puede superar el 100%")
        @Digits(integer = 3, fraction = 4, message = "La tasa admite como máximo 4 decimales")
        private Double monthlyRatePercent;
    }
}
