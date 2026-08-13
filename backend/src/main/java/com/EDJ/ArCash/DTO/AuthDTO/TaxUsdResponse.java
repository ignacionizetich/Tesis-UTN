package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Respuesta con el cálculo de impuestos sobre un monto en dólares")
public class TaxUsdResponse {
    @Schema(description = "Monto ingresado en dólares", example = "100.00")
    private double montoUsd;

    @Schema(description = "Monto convertido a pesos (USD × venta)", example = "151500.00")
    private double montoOriginal;

    @Schema(description = "Moneda de origen del cálculo", example = "USD")
    private String moneda;

    @Schema(description = "Precio del dólar venta usado para convertir (compat)", example = "1515.00")
    private double precioDolar;

    @Schema(description = "Cotización compra (dolarapi)", example = "1465.00")
    private double dolarCompra;

    @Schema(description = "Cotización venta (dolarapi)", example = "1515.00")
    private double dolarVenta;

    @Schema(description = "Nombre de la cotización (dolarapi)", example = "Oficial")
    private String nombreCotizacion;

    @Schema(description = "Casa de cambio (dolarapi)", example = "oficial")
    private String casa;

    @Schema(description = "Fecha de actualización de la cotización (dolarapi)", example = "2026-08-13T16:00:00.000Z")
    private String fechaActualizacion;

    @Schema(description = "Alicuota de IVA aplicada (porcentaje)", example = "21.0")
    private double alicuotaIva;

    @Schema(description = "IVA calculado sobre el monto en ARS", example = "31815.00")
    private double IVA;

    @Schema(description = "Total final en ARS (monto convertido + IVA)", example = "183315.00")
    private double totalFinal;
}
