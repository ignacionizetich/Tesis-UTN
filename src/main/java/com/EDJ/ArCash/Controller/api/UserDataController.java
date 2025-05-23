package com.EDJ.ArCash.Controller.api;


import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping(value = "/api/user", produces = "application/json")
public class UserDataController {

    @Autowired
    private AccountRepository accountRepository;

    @GetMapping("/data")
    public ResponseEntity<?> getUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        Optional<Account> optionalAccount =  accountRepository.findByUserIduser(user.getIduser());
        Account account = optionalAccount.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada para el usuario")
        );
        return ResponseEntity.ok(new UserDTO(user.getName(), user.getLastName(), user.getEmail(), user.getAlias(),account.getIdAccount(), account.getBalance()));
    }


    public record UserDTO(String name, String lastName, String email, String alias, Long idAccount, double balance) {
    }


}
