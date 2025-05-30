package com.EDJ.ArCash.Controller.api;


import com.EDJ.ArCash.DTO.AccountRequest;
import com.EDJ.ArCash.DTO.AccountResponse;
import com.EDJ.ArCash.DTO.AliasRequest;
import com.EDJ.ArCash.DTO.AliasResponse;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Security.JwtUtils;
import com.EDJ.ArCash.Service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/accounts", produces = "application/json")
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
                    .body(new AccountResponse(false, "El monto a ingresar no puede ser negativo.", accountRequest.getBalance()));
        }


        Object responseEntity = jwtUtils.validateAccessToken(request).getBody();

        if(responseEntity == null){
            return ResponseEntity.status(498).body(new AccountResponse(false ,"Usuario invalido",0));
        }

        Optional<Account> optionalAccount = accountService.findAccountByID(id);

        if (optionalAccount.isEmpty()) {
            return ResponseEntity.status(404).body(new AccountResponse(false, "La cuenta no existe.",0));
        } else {
            Account account = optionalAccount.get();

            if (account.getUser().getIduser().equals(responseEntity)) {
                boolean success = accountService.updateBalance(accountRequest.getBalance(), id);
                if (!success) {
                    return ResponseEntity.badRequest()
                            .body(new AccountResponse(false, "No se pudo actualizar el balance. Verifique el ID ingresado.",0));
                }

                return ResponseEntity.ok(new AccountResponse(true, "Ingreso de dinero realizado correctamente.", account.getBalance()));
            } else {
                return ResponseEntity.status(498).body(new AccountResponse(false, "El usuario no es propietario legitimo de la cuenta", 0));
            }
        }
    }


    @GetMapping("/{id}/showBalance")
    public ResponseEntity<?> getAccount(@PathVariable Long id, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }

        String token = authHeader.substring(7);
        Claims claims = JwtUtils.getClaimJWT(token);
        String userIdStr = claims.get("userID", String.class);
        Long userId = userIdStr != null ? Long.parseLong(userIdStr) : null;

        Optional<Account> optionalAccount = accountService.findAccountByID(id);
        if (optionalAccount.isPresent() && optionalAccount.get().getUser().getIduser().equals(userId)) {
            Account account = optionalAccount.get();

            Map<String, Object> response = new HashMap<>();
            response.put("balance", Double.valueOf(account.getBalance()));
            response.put("alias", account.getAccountNickname());
            response.put("cvu", account.getAccountCvu());

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(403).build();
    }

    @PutMapping("/{id}/changeAlias")
    public ResponseEntity<AliasResponse> changeAlias(@PathVariable Long id, @RequestBody AliasRequest aliasRequest, HttpServletRequest request){
        Object responseEntity = jwtUtils.validateAccessToken(request).getBody();

        if(responseEntity == null){
            return ResponseEntity.status(498).body(new AliasResponse(false, "Usuario invalido."));
        }

        return accountService.changeAlias(aliasRequest.getNewAlias(), id, responseEntity);
    }



}








