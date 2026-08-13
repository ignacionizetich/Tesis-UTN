package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.AuthDTO.AccountSearchResponse;
import com.EDJ.ArCash.DTO.AuthDTO.BuyUsdRequest;
import com.EDJ.ArCash.DTO.AuthDTO.BuyUsdResponse;
import com.EDJ.ArCash.DTO.AuthDTO.SellUsdRequest;
import com.EDJ.ArCash.DTO.AuthDTO.SellUsdResponse;
import com.EDJ.ArCash.DTO.AuthDTO.TransactionDTO;
import com.EDJ.ArCash.DTO.AuthDTO.TransactionResponse;
import com.EDJ.ArCash.DTO.AuthDTO.TranscationRequest;
import com.EDJ.ArCash.Security.CustomUserDetails;
import com.EDJ.ArCash.Service.interfaces.AccountService;
import com.EDJ.ArCash.Service.result.OwnedBuyUsdResult;
import com.EDJ.ArCash.Service.result.OwnedSellUsdResult;
import com.EDJ.ArCash.Service.result.OwnedTransferResult;
import com.EDJ.ArCash.Service.interfaces.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/transactions", produces = "application/json")
@Tag(name = "Transacciones", description = "Operaciones relacionadas con transferencias y consultas de cuentas")
public class TransactionController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public TransactionController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @PostMapping("/{id1}/transfer/{id2}")
    public ResponseEntity<TransactionResponse> transaction(
            @Parameter(description = "ID de la cuenta origen", required = true) @PathVariable Long id1,
            @Parameter(description = "ID de la cuenta destino", required = true) @PathVariable Long id2,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos de la transferencia",
                    required = true,
                    content = @Content(schema = @Schema(implementation = TranscationRequest.class))
            )
            @RequestBody TranscationRequest transcationRequest,
            @AuthenticationPrincipal CustomUserDetails principal) {

        // Inalcanzable en produccion: SecurityConfig.anyRequest().authenticated().
        // Se preserva para tests con addFilters=false (mismo criterio Fase 4).
        if (principal == null) {
            return ResponseEntity.status(498).body(new TransactionResponse(false, "Token no valido"));
        }

        OwnedTransferResult result = transactionService.transferForOwner(
                principal.getUser().getId(), id1, id2, transcationRequest.getBalance());

        return switch (result.getKind()) {
            case OK -> ResponseEntity.ok(new TransactionResponse(true, result.getMessage()));
            case ACCOUNT_NOT_FOUND -> ResponseEntity.status(404)
                    .body(new TransactionResponse(false, result.getMessage()));
            case FORBIDDEN -> ResponseEntity.status(403)
                    .body(new TransactionResponse(false, result.getMessage()));
            case FAIL -> ResponseEntity.status(400)
                    .body(new TransactionResponse(false, result.getMessage()));
        };
    }

    @GetMapping("/search/{input}")
    public ResponseEntity<?> searchAccount(
            @Parameter(description = "Alias o CVU de la cuenta", required = true) @PathVariable String input) {
        Optional<AccountSearchResponse> found = accountService.searchByAliasOrCvu(input);
        if (found.isPresent()) {
            return ResponseEntity.ok(found.get());
        }
        return ResponseEntity.status(404).body(Map.of("error", "Cuenta no encontrada."));
    }

    @GetMapping("/{id}/getTransactions")
    public List<TransactionDTO> getTransactions(
            @Parameter(description = "ID de la cuenta", required = true) @PathVariable long id){
        return transactionService.listaTransacciones(id);
    }

    @Operation(
            summary = "Comprar dólares",
            description = "Permite comprar dólares desde una cuenta en pesos. La conversión se realiza con la cotización oficial del momento."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Compra realizada exitosamente",
                    content = @Content(schema = @Schema(implementation = BuyUsdResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos o saldo insuficiente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"success\": false, \"message\": \"Saldo insuficiente en cuenta en pesos\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuario no autenticado"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cuenta no encontrada"
            )
    })
    @PostMapping("/{accountArsId}/buy-usd/{accountUsdId}")
    public ResponseEntity<?> buyUsd(
            @Parameter(description = "ID de la cuenta en pesos (origen)", required = true)
            @PathVariable Long accountArsId,
            @Parameter(description = "ID de la cuenta en dólares (destino)", required = true)
            @PathVariable Long accountUsdId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Monto en pesos a convertir",
                    required = true,
                    content = @Content(schema = @Schema(implementation = BuyUsdRequest.class))
            )
            @RequestBody BuyUsdRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        // Inalcanzable en produccion: SecurityConfig.anyRequest().authenticated().
        // Se preserva para tests con addFilters=false (mismo criterio Fase 4).
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Token no proporcionado o inválido"));
        }

        OwnedBuyUsdResult owned = transactionService.buyUsdForOwner(
                principal.getUser().getId(), accountArsId, accountUsdId, request.getAmountArs());

        return switch (owned.getKind()) {
            case ARS_NOT_FOUND -> ResponseEntity.status(404).body(owned.toErrorBody());
            case FORBIDDEN -> ResponseEntity.status(403).body(owned.toErrorBody());
            case OK -> ResponseEntity.ok(owned.getResult().toResponse());
            case FAIL -> ResponseEntity.status(400).body(owned.getResult().toErrorMap());
        };
    }

    @Operation(
            summary = "Vender dólares",
            description = "Permite vender dólares a una cuenta en pesos. La conversión usa la cotización de compra oficial del momento."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Venta realizada exitosamente",
                    content = @Content(schema = @Schema(implementation = SellUsdResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos o saldo insuficiente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"success\": false, \"message\": \"Saldo insuficiente\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuario no autenticado"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cuenta no encontrada"
            )
    })
    @PostMapping("/{accountUsdId}/sell-usd/{accountArsId}")
    public ResponseEntity<?> sellUsd(
            @Parameter(description = "ID de la cuenta en dólares (origen)", required = true)
            @PathVariable Long accountUsdId,
            @Parameter(description = "ID de la cuenta en pesos (destino)", required = true)
            @PathVariable Long accountArsId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Monto en dólares a convertir",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SellUsdRequest.class))
            )
            @RequestBody SellUsdRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        // Inalcanzable en produccion: SecurityConfig.anyRequest().authenticated().
        // Se preserva para tests con addFilters=false (mismo criterio Fase 4).
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Token no proporcionado o inválido"));
        }

        OwnedSellUsdResult owned = transactionService.sellUsdForOwner(
                principal.getUser().getId(), accountUsdId, accountArsId, request.getAmountUsd());

        return switch (owned.getKind()) {
            case USD_NOT_FOUND -> ResponseEntity.status(404).body(owned.toErrorBody());
            case FORBIDDEN -> ResponseEntity.status(403).body(owned.toErrorBody());
            case OK -> ResponseEntity.ok(owned.getResult().toResponse());
            case FAIL -> ResponseEntity.status(400).body(owned.getResult().toErrorMap());
        };
    }
}
