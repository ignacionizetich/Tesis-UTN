package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.AuthDTO.TaxPesosResponse;
import com.EDJ.ArCash.DTO.AuthDTO.TaxUsdResponse;
import com.EDJ.ArCash.Service.result.TaxCalculationResult;
import com.EDJ.ArCash.Service.interfaces.TaxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/impuestos", produces = "application/json")
@Tag(name = "Impuestos", description = "Operaciones para el cálculo de impuestos en ARS y USD")
public class TaxController {

    private final TaxService taxService;

    public TaxController(TaxService taxService) {
        this.taxService = taxService;
    }

    @Operation(
            summary = "Calcular impuestos en ARS",
            description = "Calcula los impuestos aplicables a un monto en pesos argentinos (ARS)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cálculo exitoso",
                    content = @Content(schema = @Schema(implementation = TaxPesosResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Monto inválido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"error\": \"El monto en ARS no puede ser cero o negativo.\"}"
                            )
                    )
            )
    })
    @GetMapping("/calculateARS")
    public ResponseEntity<?> calcularARS(
            @Parameter(description = "Monto en ARS", required = true, example = "10000")
            @RequestParam double montoARS) {
        TaxCalculationResult result = taxService.calcularPesosRequest(montoARS);
        return switch (result.getKind()) {
            case OK_ARS -> ResponseEntity.ok(result.getArs());
            case INVALID -> ResponseEntity.badRequest().body(result.toErrorBody());
            case OK_USD -> throw new IllegalStateException("resultado inesperado");
        };
    }

    @Operation(
            summary = "Calcular impuestos en USD",
            description = "Calcula los impuestos aplicables a un monto en dólares estadounidenses (USD)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cálculo exitoso",
                    content = @Content(schema = @Schema(implementation = TaxUsdResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Monto inválido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"error\": \"El monto en USD no puede ser cero.\"}"
                            )
                    )
            )
    })
    @GetMapping("/calculateUSD")
    public ResponseEntity<?> calcularUSD(
            @Parameter(description = "Monto en USD", required = true, example = "100")
            @RequestParam double montoUSD) {
        TaxCalculationResult result = taxService.calcularUsdRequest(montoUSD);
        return switch (result.getKind()) {
            case OK_USD -> ResponseEntity.ok(result.getUsd());
            case INVALID -> ResponseEntity.badRequest().body(result.toErrorBody());
            case OK_ARS -> throw new IllegalStateException("resultado inesperado");
        };
    }
}
