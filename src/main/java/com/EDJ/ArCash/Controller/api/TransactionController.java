package com.EDJ.ArCash.Controller.api;


import com.EDJ.ArCash.DTO.TransactionResponse;
import com.EDJ.ArCash.DTO.TranscationRequest;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Security.JwtUtils;
import com.EDJ.ArCash.Service.AccountService;
import com.EDJ.ArCash.Service.TransactionService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(value = "/api/transactions", produces = "application/json")
public class TransactionController {

    @Autowired
    private AccountService accountService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/{id1}/transfer/{id2}")
    public ResponseEntity<TransactionResponse> transaction(@PathVariable Long id1, @PathVariable Long id2, @RequestBody TranscationRequest transcationRequest, HttpServletRequest request) {

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
            Claims claims = JwtUtils.getClaimJWT(token);
            String userIdStr = claims.get("userID", String.class);
            Long userId = userIdStr != null ? Long.parseLong(userIdStr) : null;

            if (userId == null) {
                return ResponseEntity.status(498).body(new TransactionResponse(false, "Token inválido o nulo"));
            }

            Account account = optionalOrigen.get();

            if (!account.getUser().getId_user().equals(userId)) {
                return ResponseEntity.status(403).body(new TransactionResponse(false, "No tiene permiso para operar esta cuenta"));
            }


            if(transactionService.transaction(id1, id2, transcationRequest.getBalance())){
                return ResponseEntity.ok(new TransactionResponse(true, "Transferencia realizada correctamente"));
            }else{
                return ResponseEntity.status(403).body(new TransactionResponse(false, "Not enough cash, stranger."));
            }

        }

    }


}
