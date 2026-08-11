package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.AuthDTO.BuyUsdRequest;
import com.EDJ.ArCash.DTO.AuthDTO.BuyUsdResponse;
import com.EDJ.ArCash.DTO.AuthDTO.TransactionDTO;
import com.EDJ.ArCash.DTO.AuthDTO.TransactionResponse;
import com.EDJ.ArCash.DTO.AuthDTO.TranscationRequest;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.FavoriteContact;
import com.EDJ.ArCash.Security.CustomUserDetails;
import com.EDJ.ArCash.Service.AccountService;
import com.EDJ.ArCash.Service.BuyUsdResult;
import com.EDJ.ArCash.Service.FavoriteContactService;
import com.EDJ.ArCash.Service.TransactionService;
import com.EDJ.ArCash.Service.TransferOperationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/transactions", produces = "application/json")
@Tag(name = "Transacciones", description = "Operaciones relacionadas con transferencias y consultas de cuentas")
public class TransactionController {

    @Autowired
    private AccountService accountService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private FavoriteContactService favoriteContactService;

    @PostMapping("/{id1}/transfer/{id2}")
    @Transactional
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

        Optional<Account> optionalOrigen = accountService.findAccountByID(id1);
        Optional<Account> optionalDestination = accountService.findAccountByID(id2);

        if (optionalOrigen.isEmpty() || optionalDestination.isEmpty()) {
            return ResponseEntity.status(404).body(new TransactionResponse(false, "No se pudo encontrar la cuenta de orig."));
        }

        // Inalcanzable en produccion: SecurityConfig.anyRequest().authenticated().
        // Se preserva para tests con addFilters=false (mismo criterio Fase 4).
        if (principal == null) {
            return ResponseEntity.status(498).body(new TransactionResponse(false, "Token no valido"));
        }

        Long userId = principal.getUser().getId();
        Account account = optionalOrigen.get();

        if (!account.getUser().getId().equals(userId)) {
            return ResponseEntity.status(403).body(new TransactionResponse(false, "No tiene permiso para operar esta cuenta"));
        }

        TransferOperationResult result = transactionService.transactionWithDetails(id1, id2, transcationRequest.getBalance());

        if (result.isSuccess()) {
            updateLastUsedForFavoriteContact(userId, id2);
            return ResponseEntity.ok(new TransactionResponse(true, "Transferencia realizada correctamente"));
        } else {
            String errorMessage = result.getMessage() != null ?
                    result.getMessage() : "Not enough cash, stranger.";
            return ResponseEntity.status(400).body(new TransactionResponse(false, errorMessage));
        }
    }

    private void updateLastUsedForFavoriteContact(Long userId, Long destinationAccountId) {
        try {
            List<FavoriteContact> userFavorites = favoriteContactService.getFavoriteContactsByUser(userId);

            Optional<FavoriteContact> matchingFavorite = userFavorites.stream()
                    .filter(favorite -> favorite.getFavoriteAccount().getIdAccount().equals(destinationAccountId))
                    .findFirst();

            if (matchingFavorite.isPresent()) {
                favoriteContactService.updateLastUsedForContact(matchingFavorite.get().getId());
            }
        } catch (Exception e) {
            System.out.println("Error actualizando lastUsed para contacto favorito: " + e.getMessage());
        }
    }

    @GetMapping("/search/{input}")
    public ResponseEntity<?> searchAccount(
            @Parameter(description = "Alias o CVU de la cuenta", required = true) @PathVariable String input) {
        Optional<Account> account = accountService.encontrarCuentaPorAlias(input);
        if (account.isEmpty()) {
            account = accountService.encontrarCuentaPorCvu(input);
        }

        if (account.isPresent()) {
            Account acc = account.get();
            Map<String, Object> result = new HashMap<>();
            result.put("idaccount", acc.getIdAccount());
            result.put("alias", acc.getAccountNickname());
            result.put("cvu", acc.getAccountCvu());
            result.put("user", Map.of(
                    "nombre", acc.getUser().getName(),
                    "apellido", acc.getUser().getLastName(),
                    "dni", acc.getUser().getDni()
            ));
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(404).body(Map.of("error", "Cuenta no encontrada."));
        }
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

        Long userId = principal.getUser().getId();

        Optional<Account> accountArsOpt = accountService.findAccountByID(accountArsId);
        if (accountArsOpt.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Cuenta en pesos no encontrada"));
        }

        Account accountArs = accountArsOpt.get();
        if (!accountArs.getUser().getId().equals(userId)) {
            return ResponseEntity.status(403)
                    .body(Map.of("success", false, "message", "No tiene permiso para operar esta cuenta"));
        }

        BuyUsdResult result = transactionService.buyUsd(accountArsId, accountUsdId, request.getAmountArs());

        if (result.isSuccess()) {
            return ResponseEntity.ok(new BuyUsdResponse(
                    true,
                    result.getMessage(),
                    result.getAmountArs(),
                    result.getAmountUsd(),
                    result.getExchangeRate(),
                    result.getTaxAmount(),
                    result.getTaxPercentage(),
                    result.getTotalDebitado(),
                    result.getNewBalanceArs(),
                    result.getNewBalanceUsd()
            ));
        } else {
            return ResponseEntity.status(400).body(result.toErrorMap());
        }
    }
}
