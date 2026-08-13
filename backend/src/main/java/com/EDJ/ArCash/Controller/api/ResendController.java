package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Service.interfaces.AuthService;
import com.EDJ.ArCash.Service.result.ResendEmailResult;
import com.EDJ.ArCash.Service.interfaces.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/resend")
public class ResendController {

    private final UserService userService;
    private final AuthService authService;

    public ResendController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/validation")
    public ResponseEntity<Map<String, Object>> resendValidationEmail(@RequestParam("email") String email) {
        ResendEmailResult result = userService.resendValidationEmailRequest(email);
        return switch (result.getKind()) {
            case OK -> ResponseEntity.ok(result.toBody());
            case BAD_REQUEST -> ResponseEntity.badRequest().body(result.toBody());
            case ERROR -> ResponseEntity.status(500).body(result.toBody());
        };
    }

    @PostMapping("/password-recovery")
    public ResponseEntity<Map<String, Object>> resendPasswordRecovery(@RequestParam("email") String email) {
        ResendEmailResult result = authService.resendPasswordRecoveryEmail(email);
        return switch (result.getKind()) {
            case OK -> ResponseEntity.ok(result.toBody());
            case BAD_REQUEST -> ResponseEntity.badRequest().body(result.toBody());
            case ERROR -> ResponseEntity.status(500).body(result.toBody());
        };
    }
}
