package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Service.UserService;
import com.EDJ.ArCash.Service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/resend")
public class ResendController {

    private static final String VALIDATION_MESSAGE =
            "Si el email corresponde a una cuenta pendiente de validación, te enviamos un nuevo enlace.";
    private static final String PASSWORD_RECOVERY_MESSAGE =
            "Si el email está registrado, te enviamos un enlace de recuperación.";

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    /**
     * Reenvía el enlace de validación de email.
     * Respuesta idéntica ante email inexistente / ya validado / enviado (anti-enumeration).
     */
    @PostMapping("/validation")
    public ResponseEntity<Map<String, Object>> resendValidationEmail(@RequestParam("email") String email) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "El email es requerido.");
                return ResponseEntity.badRequest().body(response);
            }

            userService.resendValidationEmail(email.trim());

            response.put("success", true);
            response.put("message", VALIDATION_MESSAGE);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor. Inténtalo de nuevo.");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Reenvía el enlace de recuperación de contraseña.
     * Respuesta idéntica ante email inexistente / enviado (anti-enumeration).
     */
    @PostMapping("/password-recovery")
    public ResponseEntity<Map<String, Object>> resendPasswordRecovery(@RequestParam("email") String email) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "El email es requerido.");
                return ResponseEntity.badRequest().body(response);
            }

            authService.resendPasswordRecovery(email.trim());

            response.put("success", true);
            response.put("message", PASSWORD_RECOVERY_MESSAGE);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor. Inténtalo de nuevo.");
            return ResponseEntity.status(500).body(response);
        }
    }
}
