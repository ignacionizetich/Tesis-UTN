package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Service.result.EmailActivationResult;
import com.EDJ.ArCash.Service.interfaces.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/auth")
public class TokenController {

    private final UserService userService;

    public TokenController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateUser(
            @RequestParam(value = "token", required = false) String tokenValue) {
        EmailActivationResult result = userService.activateWithToken(tokenValue);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());

        return switch (result.getKind()) {
            case OK -> ResponseEntity.ok(response);
            case MISSING_TOKEN, INVALID, ALREADY_USED, EXPIRED -> ResponseEntity.badRequest().body(response);
        };
    }
}
