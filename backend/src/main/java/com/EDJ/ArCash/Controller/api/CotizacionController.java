package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.ApiCalloutDTO.ApiUsdResponse;
import com.EDJ.ArCash.DTO.AuthDTO.CotizacionDolarResponse;
import com.EDJ.ArCash.Service.interfaces.CotizationUsdService;
import com.EDJ.ArCash.exception.personalizated.ExchangeRateUnavailableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/cotizacion", produces = "application/json")
@Tag(name = "Cotización", description = "Cotización del dólar oficial (dolarapi)")
public class CotizacionController {

    private final CotizationUsdService cotizationUsdService;

    public CotizacionController(CotizationUsdService cotizationUsdService) {
        this.cotizationUsdService = cotizationUsdService;
    }

    @Operation(
            summary = "Obtener cotización del dólar oficial",
            description = "Devuelve compra, venta y metadatos cacheados desde dolarapi.com/v1/dolares/oficial."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cotización disponible",
                    content = @Content(schema = @Schema(implementation = CotizacionDolarResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Proveedor de cotización no disponible",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"error\": \"No se pudo obtener la cotizacion del dolar.\"}")
                    )
            )
    })
    @GetMapping("/dolar")
    public ResponseEntity<?> obtenerDolarOficial() {
        try {
            ApiUsdResponse snap = cotizationUsdService.obtenerSnapshot();
            CotizacionDolarResponse body = CotizacionDolarResponse.builder()
                    .moneda(snap.getMoneda())
                    .casa(snap.getCasa())
                    .nombre(snap.getNombre())
                    .compra(snap.getCompra())
                    .venta(snap.getVenta())
                    .fechaActualizacion(snap.getFechaActualizacion())
                    .build();
            return ResponseEntity.ok(body);
        } catch (ExchangeRateUnavailableException ex) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", ex.getMessage() != null
                            ? ex.getMessage()
                            : "No se pudo obtener la cotizacion del dolar."
            ));
        }
    }
}
