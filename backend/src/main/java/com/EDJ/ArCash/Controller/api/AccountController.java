package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.AuthDTO.*;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Security.CustomUserDetails;
import com.EDJ.ArCash.Service.AccountService;
import com.EDJ.ArCash.Service.AliasChangeResult;
import com.EDJ.ArCash.Service.SessionService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/accounts", produces = "application/json")
@Tag(name = "Cuentas", description = "Operaciones sobre cuentas del usuario")
public class AccountController {

    private final AccountService accountService;
    private final SessionService sessionService;
    private final AliasResponseFactory aliasResponseFactory;

    public AccountController(AccountService accountService, SessionService sessionService, AliasResponseFactory aliasResponseFactory) {
        this.accountService = accountService;
        this.sessionService = sessionService;
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

        if (accountRequest.getBalance() < 0) {
            return ResponseEntity.badRequest()
                    .body(new AccountResponse(false, "El monto a ingresar no puede ser negativo.", accountRequest.getBalance()));
        }

        Long userId = principal.getUser().getId();

        if (!sessionService.tieneSesionActiva(userId)) {
            return ResponseEntity.status(498).body(new AccountResponse(false ,"Usuario invalido",0));
        }

        Optional<Account> optionalAccount = accountService.findAccountByID(id);

        if (optionalAccount.isEmpty()) {
            return ResponseEntity.status(404).body(new AccountResponse(false, "La cuenta no existe.",0));
        } else {
            Account account = optionalAccount.get();

            if (account.getUser().getId().equals(userId)) {
                boolean success = accountService.updateBalance(accountRequest.getBalance(), id);
                if (!success) {
                    return ResponseEntity.status(404)
                            .body(new AccountResponse(false, "No se pudo actualizar el balance. Verifique el ID ingresado.",0));
                }

                return ResponseEntity.ok(new AccountResponse(true, "Ingreso de dinero realizado correctamente.", account.getBalance()));
            } else {
                return ResponseEntity.status(498).body(new AccountResponse(false, "El usuario no es propietario legitimo de la cuenta", 0));
            }
        }
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
        Long userId = principal.getUser().getId();

        Optional<Account> optionalAccount = accountService.findAccountByID(id);
        if (optionalAccount.isPresent() && optionalAccount.get().getUser().getId().equals(userId)) {
            Account account = optionalAccount.get();

            Map<String, Object> response = new HashMap<>();
            response.put("balance", account.getBalance());
            response.put("alias", account.getAccountNickname());
            response.put("cvu", account.getAccountCvu());

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(403)
                .body(Map.of("error", "El usuario no es propietario de la cuenta"));
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

        if(!sessionService.tieneSesionActiva(userId)){
            return ResponseEntity.status(498).body(new AliasResponse(false, "Usuario invalido."));
        }

        AliasChangeResult resultado = accountService.changeAlias(aliasRequest.getNewAlias(), id, userId);

        return switch (resultado) {
            case OK -> ResponseEntity.ok(
                    aliasResponseFactory.createSuccessResponse("Alias actualizado exitosamente."));
            case FORMATO_INVALIDO -> ResponseEntity.status(400).body(
                    aliasResponseFactory.createErrorResponse("Formato de alias inválido. Debe tener entre 4 y 25 caracteres, solo letras, números y puntos, al menos un punto en el medio, no puede ser solo números ni tener '..'."));
            case CUENTA_NO_ENCONTRADA -> ResponseEntity.status(498).body(
                    aliasResponseFactory.createErrorResponse("Cuenta no encontrada."));
            case NO_ES_PROPIETARIO -> ResponseEntity.status(403).body(
                    aliasResponseFactory.createErrorResponse("No tienes permisos para hacer eso."));
            case ALIAS_EN_USO -> ResponseEntity.status(403).body(
                    aliasResponseFactory.createErrorResponse("Alias actualmente en uso."));
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
        Long userId = principal.getUser().getId();

        Optional<Account> optionalAccount = accountService.findAccountByID(id);

        if (optionalAccount.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Cuenta no encontrada"));
        }

        Account account = optionalAccount.get();
        if (!account.getUser().getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "El usuario no es propietario de la cuenta"));
        }


        User user = account.getUser();

        Map<String, Object> qrData = new HashMap<>();
        qrData.put("walletApp", "ArCashV1");
        qrData.put("accountId", account.getIdAccount());
        qrData.put("accountAlias", account.getAccountNickname());
        qrData.put("receiverName", user.getName() + " " + user.getLastName());
        qrData.put("dni", user.getDni());
        qrData.put("email", user.getEmail());
        if ("ARS".equalsIgnoreCase(account.getAccountType().toString())) {
            qrData.put("currency", "ARS");
        } else {
            qrData.put("currency", account.getAccountType().toString());
        }


        return ResponseEntity.ok(qrData);
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
    public ResponseEntity<?> openUsdAccount(@AuthenticationPrincipal CustomUserDetails principal){

        try{
            User authUser = principal.getUser();

            Account usdAccount = accountService.openUsdAccount(authUser);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cuenta en dólares creada exitosamente",
                    "accountId", usdAccount.getIdAccount(),
                    "accountAlias", usdAccount.getAccountNickname(),
                    "currency", "USD"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error al crear cuenta en dólares: " + e.getMessage()));
        }
    }

}
