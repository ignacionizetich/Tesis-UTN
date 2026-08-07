package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Service.UserService;
import com.EDJ.ArCash.Service.ValidationTokenService;
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

    @Autowired
    private UserService userService;
    
    @Autowired
    private ValidationTokenService validationTokenService;
    
    @Autowired
    private AuthService authService;

    /**
     * Reenvía el enlace de validación de email a un usuario no validado
     */
    @PostMapping("/validation")
    public ResponseEntity<Map<String, Object>> resendValidationEmail(@RequestParam("email") String email) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Verificar que el email no esté vacío
            if (email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "El email es requerido.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Intentar reenviar el enlace de validación
            boolean sent = userService.resendValidationEmail(email.trim());
            
            if (sent) {
                response.put("success", true);
                response.put("message", "Se ha enviado un nuevo enlace de validación a tu email.");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "No se pudo enviar el enlace. Verifica que el email sea correcto y que la cuenta no esté ya validada.");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor. Inténtalo de nuevo.");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Reenvía el enlace de recuperación de contraseña
     */
    @PostMapping("/password-recovery")
    public ResponseEntity<Map<String, Object>> resendPasswordRecovery(@RequestParam("email") String email) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Verificar que el email no esté vacío
            if (email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "El email es requerido.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Intentar reenviar el enlace de recuperación
            boolean sent = authService.resendPasswordRecovery(email.trim());
            
            if (sent) {
                response.put("success", true);
                response.put("message", "Se ha enviado un nuevo enlace de recuperación a tu email.");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "No se pudo enviar el enlace. Verifica que el email sea correcto y que la cuenta exista.");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor. Inténtalo de nuevo.");
            return ResponseEntity.status(500).body(response);
        }
    }
}
