package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Respuesta de venta de dólares")
public class SellUsdResponse {

    @Schema(description = "Indica si la operación fue exitosa", example = "true")
    private boolean success;

    @Schema(description = "Mensaje descriptivo de la operación", example = "Venta de dólares exitosa")
    private String message;

    @Schema(description = "Monto en dólares vendidos (base, sin comisión)", example = "100.00")
    private double amountUsd;

    @Schema(description = "Monto en pesos acreditado", example = "100000.00")
    private double amountArs;

    @Schema(description = "Tasa de cambio aplicada (compra)", example = "1000.00")
    private double exchangeRate;

    @Schema(description = "Monto de comisión en dólares", example = "3.00")
    private double taxAmount;

    @Schema(description = "Porcentaje de comisión", example = "3.0")
    private double taxPercentage;

    @Schema(description = "Total debitado en dólares (incluye comisión)", example = "103.00")
    private double totalDebitado;

    @Schema(description = "Nuevo balance en pesos", example = "101000.00")
    private double newBalanceArs;

    @Schema(description = "Nuevo balance en dólares", example = "97.00")
    private double newBalanceUsd;
}
