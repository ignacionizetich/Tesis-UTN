package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.AuthDTO.*;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Security.CustomUserDetails;
import com.EDJ.ArCash.Service.result.AccountBalanceView;
import com.EDJ.ArCash.Service.interfaces.AccountService;
import com.EDJ.ArCash.Service.result.AliasChangeResult;
import com.EDJ.ArCash.Service.result.DepositResult;
import com.EDJ.ArCash.Service.result.OpenUsdResult;
import com.EDJ.ArCash.Service.result.QrDataResult;
import com.EDJ.ArCash.factory.AliasResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping(value = "/api/accounts", produces = "application/json")
@Tag(name = "Cuentas", description = "Operaciones sobre cuentas del usuario")
public class AccountController {

    private final AccountService accountService;
    private final AliasResponseFactory aliasResponseFactory;

    public AccountController(AccountService accountService, AliasResponseFactory aliasResponseFactory) {
        this.accountService = accountService;
        this.aliasResponseFactory = aliasResponseFactory;
    }

    @Operation(
            summary = "Actualizar balance de la cuenta",
            description = "Permite ingresar dinero a la cuenta del usuario."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Balance actualizado correctamente",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Monto negativo o inválido",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cuenta no encontrada",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))
            ),
            @ApiResponse(
                    responseCode = "498",
                    description = "Usuario inválido o no autorizado",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))
            )
    })
    @PutMapping("/{id}/balance")
    public ResponseEntity<AccountResponse> updateBalance(
            @PathVariable Long id,
            @RequestBody AccountRequest accountRequest,
            @AuthenticationPrincipal CustomUserDetails principal) {

        DepositResult resultado = accountService.deposit(
                id, principal.getUser().getId(), accountRequest.getBalance());

        AccountResponse body = resultado.toAccountResponse();
        return switch (resultado.getKind()) {
            case OK -> ResponseEntity.ok(body);
            case MONTO_NEGATIVO -> ResponseEntity.badRequest().body(body);
            case CUENTA_NO_EXISTE, UPDATE_FALLIDO -> ResponseEntity.status(404).body(body);
            case NO_ES_PROPIETARIO -> ResponseEntity.status(498).body(body);
        };
    }

    @Operation(
            summary = "Obtener información de la cuenta",
            description = "Devuelve el balance, alias y CVU de la cuenta si el usuario es propietario."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Datos de la cuenta obtenidos correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"balance\": 1000.0, \"alias\": \"mi.alias.cuenta\", \"cvu\": \"0001234567890123456789\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token no proporcionado o inválido"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario no es propietario de la cuenta",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"error\": \"El usuario no es propietario de la cuenta\"}"
                            )
                    )
            )
    })
    @GetMapping("/{id}/showBalance")
    public ResponseEntity<?> getAccount(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        Optional<AccountBalanceView> vista =
                accountService.getOwnedBalance(id, principal.getUser().getId());

        if (vista.isEmpty()) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "El usuario no es propietario de la cuenta"));
        }

        return ResponseEntity.ok(vista.get().toResponseMap());
    }

    @Operation(
            summary = "Cambiar alias de la cuenta",
            description = "Permite al usuario cambiar el alias de su cuenta."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Alias cambiado correctamente",
                    content = @Content(schema = @Schema(implementation = AliasResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Alias inválido",
                    content = @Content(schema = @Schema(implementation = AliasResponse.class))
            ),
            @ApiResponse(
                    responseCode = "498",
                    description = "Usuario inválido o no autorizado",
                    content = @Content(schema = @Schema(implementation = AliasResponse.class))
            )
    })
    @PutMapping("/{id}/changeAlias")
    public ResponseEntity<AliasResponse> changeAlias(
            @PathVariable Long id,
            @RequestBody AliasRequest aliasRequest,
            @AuthenticationPrincipal CustomUserDetails principal){
        Long userId = principal.getUser().getId();

        AliasChangeResult resultado = accountService.changeAlias(aliasRequest.getNewAlias(), id, userId);

        return switch (resultado) {
            case OK -> ResponseEntity.ok(
                    aliasResponseFactory.createSuccessResponse(resultado.getMessage()));
            case FORMATO_INVALIDO -> ResponseEntity.status(400).body(
                    aliasResponseFactory.createErrorResponse(resultado.getMessage()));
            case CUENTA_NO_ENCONTRADA -> ResponseEntity.status(498).body(
                    aliasResponseFactory.createErrorResponse(resultado.getMessage()));
            case NO_ES_PROPIETARIO, ALIAS_EN_USO -> ResponseEntity.status(403).body(
                    aliasResponseFactory.createErrorResponse(resultado.getMessage()));
        };
    }

    @Operation(
            summary = "Obtener datos para el código QR",
            description = "Devuelve la información necesaria para generar un código QR para recibir transferencias."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Datos para el QR obtenidos correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"walletApp\":\"ArCashV1\",\"accountId\":123,\"accountAlias\":\"juan.perez.arcash\",\"receiverName\":\"Juan Pérez\",\"dni\":\"12345678\",\"currency\":\"ARS\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token no proporcionado o inválido"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario no es propietario de la cuenta"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cuenta no encontrada"
            )
    })
    @GetMapping("/{id}/qr-data")
    public ResponseEntity<Map<String, Object>> getQrData(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        QrDataResult resultado = accountService.getQrDataForOwner(id, principal.getUser().getId());

        return switch (resultado.getKind()) {
            case CUENTA_NO_ENCONTRADA -> ResponseEntity.status(404)
                    .body(Map.of("error", "Cuenta no encontrada"));
            case NO_ES_PROPIETARIO -> ResponseEntity.status(403)
                    .body(Map.of("error", "El usuario no es propietario de la cuenta"));
            case OK -> ResponseEntity.ok(resultado.getPayload().toResponseMap());
        };
    }

    @Operation(
            summary = "Listar las cuentas del usuario",
            description = "Devuelve todas las cuentas del usuario autenticado, en cualquiera de sus monedas."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cuentas obtenidas correctamente",
                    content = @Content(schema = @Schema(implementation = UserAccountsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuario no autenticado"
            )
    })
    @GetMapping("/user-accounts")
    public ResponseEntity<UserAccountsResponse> getUserAccounts(@AuthenticationPrincipal CustomUserDetails principal) {
        List<Account> cuentas = accountService.findAccountsByUser(principal.getUser().getId());

        return ResponseEntity.ok(UserAccountsResponse.from(cuentas));
    }

    @PostMapping("/usd")
    public ResponseEntity<?> openUsdAccount(@AuthenticationPrincipal CustomUserDetails principal) {
        OpenUsdResult resultado = accountService.openUsdAccount(principal.getUser());
        return switch (resultado.getKind()) {
            case OK -> ResponseEntity.ok(resultado.toSuccessBody());
            case ALREADY_EXISTS -> ResponseEntity.status(409).body(resultado.toErrorBody());
            case ERROR -> ResponseEntity.status(500).body(resultado.toErrorBody());
        };
    }

}
