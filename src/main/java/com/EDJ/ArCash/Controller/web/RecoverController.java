package com.EDJ.ArCash.Controller.web;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.EDJ.ArCash.Service.AuthService;
import com.EDJ.ArCash.Service.CredentialsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class RecoverController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CredentialsService credentialsService;

    @Operation(
            summary = "Validar token y redirigir",
            description = "Valida el token de recuperación y redirige al formulario de reset si es válido."
    )
    @GetMapping("/validate-request")
    public RedirectView validateTokenAndRedirect(@RequestParam("token") String token) {
        if (authService.tokenValido(token)) {
            // Token válido, redirigir a Angular con el token
            return new RedirectView("http://localhost:4200/reset-password?token=" + token);
        } else {
            // Token inválido, redirigir a página de error sin el token
            return new RedirectView("http://localhost:4200/404");
        }
    }

    @Operation(
            summary = "Validar token de recuperación",
            description = "Valida si un token de recuperación es válido y no ha sido usado."
    )
    @GetMapping("/validate-recovery-token")
    public ResponseEntity<Map<String, Object>> validateRecoveryToken(@RequestParam("token") String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isValid = authService.tokenValido(token);
            
            if (isValid) {
                response.put("valid", true);
                response.put("message", "Enlace de recuperación válido");
                return ResponseEntity.ok(response);
            } else {
                response.put("valid", false);
                response.put("message", "El enlace de recuperación es inválido, ha expirado o ya fue utilizado");
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            response.put("valid", false);
            response.put("message", "Error al validar el enlace de recuperación");
            return ResponseEntity.status(500).body(response);
        }
    }

    @Operation(
            summary = "Restablecer contraseña",
            description = "Permite al usuario restablecer su contraseña usando un token válido."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña restablecida exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error en los datos proporcionados"),
            @ApiResponse(responseCode = "401", description = "Token inválido o expirado")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword) {

        Map<String, Object> response = new HashMap<>();

        try {
            String resultado = credentialsService.actualizarPassword(token, password, confirmPassword);

            if (resultado.contains("exitosamente")) {
                response.put("success", true);
                response.put("message", resultado);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", resultado);
                
                // Determinar el código de estado basado en el mensaje
                if (resultado.contains("enlace de recuperación no es válido") || 
                    resultado.contains("ya fue utilizado") || 
                    resultado.contains("ha expirado")) {
                    return ResponseEntity.status(401).body(response);
                } else {
                    return ResponseEntity.status(400).body(response);
                }
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor. Por favor, inténtalo de nuevo.");
            return ResponseEntity.status(500).body(response);
        }
    }

}
