package com.EDJ.ArCash.Controller.api;


import com.EDJ.ArCash.DTO.AccountRequest;
import com.EDJ.ArCash.DTO.AccountResponse;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Security.JwtUtils;
import com.EDJ.ArCash.Service.AccountService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(value = "/accounts", produces = "application/json")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private JwtUtils jwtUtils;


    /// Contiene los endpoints que interactuan con la cuenta propia del cliente. Sólo involucra un cliente, el propietario.
    @PutMapping("/{id}/balance")
    public ResponseEntity<AccountResponse> updateBalance(@PathVariable Long id, @RequestBody AccountRequest accountRequest, HttpServletRequest request) {


        if (accountRequest.getBalance() < 0) {
            return ResponseEntity.badRequest()
                    .body(new AccountResponse(false, "El monto a ingresar no puede ser negativo."));
        }


        // 1. Obtener token de la cabecera Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(498).body(new AccountResponse(false, "Token no proporcionado"));
        }
        String token = authHeader.substring(7);

        // 2. Extraer claims
        Claims claims = JwtUtils.getClaimJWT(token);
        String userIdStr = claims.get("userID", String.class);
        Long userId = userIdStr != null ? Long.parseLong(userIdStr) : null;

        if (userId == null) {
            return ResponseEntity.status(498).body(new AccountResponse(false, "Token inválido o sin userId"));
        }

        Optional<Account> optionalAccount = accountService.findAccountByID(id);

        if (optionalAccount.isEmpty()) {
            return ResponseEntity.status(404).body(new AccountResponse(false, "La cuenta no existe."));
        } else {
            Account account = optionalAccount.get();

            if (account.getUser().getId_user().equals(userId)) {
                boolean success = accountService.updateBalance(accountRequest.getBalance(), id);
                if (!success) {
                    return ResponseEntity.badRequest()
                            .body(new AccountResponse(false, "No se pudo actualizar el balance. Verifique el ID ingresado."));
                }

                return ResponseEntity.ok(new AccountResponse(true, "Ingreso de dinero realizado correctamente."));
            } else {
                return ResponseEntity.status(498).body(new AccountResponse(false, "El usuario no es propietario legitimo de la cuenta"));
            }
        }
    }


}








