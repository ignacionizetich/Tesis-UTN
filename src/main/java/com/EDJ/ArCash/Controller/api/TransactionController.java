package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.AuthDTO.TransactionDTO;
import com.EDJ.ArCash.DTO.AuthDTO.TransactionResponse;
import com.EDJ.ArCash.DTO.AuthDTO.TranscationRequest;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.FavoriteContact;
import com.EDJ.ArCash.Security.JwtUtils;
import com.EDJ.ArCash.Service.AccountService;
import com.EDJ.ArCash.Service.FavoriteContactService;
import com.EDJ.ArCash.Service.TransactionService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    @Autowired
    private JwtUtils jwtUtils;

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
            HttpServletRequest request) {

        Optional<Account> optionalOrigen = accountService.findAccountByID(id1);
        Optional<Account> optionalDestination = accountService.findAccountByID(id2);

        if (optionalOrigen.isEmpty() || optionalDestination.isEmpty()) {
            return ResponseEntity.status(404).body(new TransactionResponse(false, "No se pudo encontrar la cuenta de orig."));
        } else {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(498).body(new TransactionResponse(false, "Token no valido"));
            }

            String token = authHeader.substring(7);
            Claims claims = jwtUtils.getClaimJWT(token);
            String userIdStr = claims.get("userID", String.class);
            Long userId = userIdStr != null ? Long.parseLong(userIdStr) : null;

            if (userId == null) {
                return ResponseEntity.status(498).body(new TransactionResponse(false, "Token inválido o nulo"));
            }

            Account account = optionalOrigen.get();

            if (!account.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(new TransactionResponse(false, "No tiene permiso para operar esta cuenta"));
            }

            if(transactionService.transaction(id1, id2, transcationRequest.getBalance())){

                // NUEVO: Actualizar lastUsed si se transfiere a un contacto favorito
                updateLastUsedForFavoriteContact(userId, id2);

                return ResponseEntity.ok(new TransactionResponse(true, "Transferencia realizada correctamente"));
            }else{
                return ResponseEntity.status(403).body(new TransactionResponse(false, "Not enough cash, stranger."));
            }
        }
    }

    // NUEVO: Método privado para actualizar lastUsed
    private void updateLastUsedForFavoriteContact(Long userId, Long destinationAccountId) {
        try {
            // Buscar si la cuenta de destino es un contacto favorito del usuario
            List<FavoriteContact> userFavorites = favoriteContactService.getFavoriteContactsByUser(userId);

            Optional<FavoriteContact> matchingFavorite = userFavorites.stream()
                    .filter(favorite -> favorite.getFavoriteAccount().getIdAccount().equals(destinationAccountId))
                    .findFirst();

            if (matchingFavorite.isPresent()) {
                favoriteContactService.updateLastUsedForContact(matchingFavorite.get().getId());
            }
        } catch (Exception e) {
            // Log del error pero no fallar la transferencia
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
}