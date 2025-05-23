package com.EDJ.ArCash.Controller.api;


import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/user", produces = "application/json")
public class UserDataController {


    @GetMapping("/data")
    public ResponseEntity<?> getUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        return ResponseEntity.ok(new UserDTO(user.getName(), user.getLastName(), user.getEmail(), user.getAlias()));
    }


    public record UserDTO(String name, String lastName, String email, String alias) {
    }


}
