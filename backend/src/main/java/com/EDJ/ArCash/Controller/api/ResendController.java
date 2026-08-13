package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Service.AuthService;
import com.EDJ.ArCash.Service.ResendEmailResult;
import com.EDJ.ArCash.Service.UserService;
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

    /**
     * Reenvía el enlace de validación de email.
     * Respuesta idéntica ante email inexistente / ya validado / enviado (anti-enumeration).
     */
    @PostMapping("/validation")
    public ResponseEntity<Map<String, Object>> resendValidationEmail(@RequestParam("email") String email) {
        ResendEmailResult result = userService.resendValidationEmailRequest(email);
        return switch (result.getKind()) {
            case OK -> ResponseEntity.ok(result.toBody());
            case BAD_REQUEST -> ResponseEntity.badRequest().body(result.toBody());
            case ERROR -> ResponseEntity.status(500).body(result.toBody());
        };
    }

    /**
     * Reenvía el enlace de recuperación de contraseña.
     * Respuesta idéntica ante email inexistente / enviado (anti-enumeration).
     */
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
