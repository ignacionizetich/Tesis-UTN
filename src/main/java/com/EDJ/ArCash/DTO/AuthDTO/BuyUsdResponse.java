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
@Schema(description = "Respuesta de compra de dólares")
public class BuyUsdResponse {
    
    @Schema(description = "Indica si la operación fue exitosa", example = "true")
    private boolean success;
    
    @Schema(description = "Mensaje descriptivo de la operación", example = "Compra de dólares exitosa")
    private String message;
    
    @Schema(description = "Monto en pesos debitado (sin impuestos)", example = "10000.00")
    private double amountArs;
    
    @Schema(description = "Monto en dólares acreditado", example = "10.00")
    private double amountUsd;
    
    @Schema(description = "Tasa de cambio aplicada", example = "1000.00")
    private double exchangeRate;
    
    @Schema(description = "Monto de impuestos", example = "6000.00")
    private double taxAmount;
    
    @Schema(description = "Porcentaje de impuesto", example = "60.0")
    private double taxPercentage;
    
    @Schema(description = "Total debitado (incluye impuestos)", example = "16000.00")
    private double totalDebitado;
    
    @Schema(description = "Nuevo balance en pesos", example = "5000.00")
    private double newBalanceArs;
    
    @Schema(description = "Nuevo balance en dólares", example = "10.00")
    private double newBalanceUsd;
}
