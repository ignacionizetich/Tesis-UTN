package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cotización del dólar oficial (dolarapi.com)")
public class CotizacionDolarResponse {
    @Schema(description = "Moneda", example = "USD")
    private String moneda;

    @Schema(description = "Casa de cambio", example = "oficial")
    private String casa;

    @Schema(description = "Nombre de la cotización", example = "Oficial")
    private String nombre;

    @Schema(description = "Valor de compra", example = "1465.00")
    private double compra;

    @Schema(description = "Valor de venta (usado para comprar USD)", example = "1515.00")
    private double venta;

    @Schema(description = "Fecha de última actualización del proveedor", example = "2026-08-13T16:00:00.000Z")
    private String fechaActualizacion;
}
