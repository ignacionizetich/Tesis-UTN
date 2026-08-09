package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Models.ValidationToken;
import com.EDJ.ArCash.Service.UserService;
import com.EDJ.ArCash.Service.ValidationTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/auth") // <-- CAMBIADO: Unificado con el otro controller
public class TokenController {

    @Autowired
    private ValidationTokenService validationTokenService;

    @Autowired
    private UserService userService;

    @GetMapping("/validate") // <-- AÑADIDO: La ruta específica
    // RUTA FINAL: /api/auth/validate
    public ResponseEntity<Map<String, Object>> validateUser(@RequestParam(value = "token", required = false) String tokenValue) {
        Map<String, Object> response = new HashMap<>();

        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Token no proporcionado");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<ValidationToken> optionalToken = validationTokenService.buscarToken(tokenValue);

        if (optionalToken.isEmpty()) {
            response.put("success", false);
            response.put("message", "El enlace de verificación no es válido o no existe.");
            return ResponseEntity.badRequest().body(response);

        } else {
            ValidationToken token = optionalToken.get();

            // Verificar si el token ya fue usado
            if (token.isUsed()) {
                response.put("success", false);
                response.put("message", "Este enlace de verificación ya fue utilizado. Tu cuenta ya está activada.");
                return ResponseEntity.badRequest().body(response);

                // Verificar si el token ha expirado
            } else if (token.getExpirationDate().isBefore(LocalDateTime.now())) {
                response.put("success", false);
                response.put("message", "El enlace de verificación ha expirado. Solicita un nuevo enlace de activación.");
                return ResponseEntity.badRequest().body(response);

            } else {
                // validarUsuario marca el token como usado
                userService.validarUsuario(token.getUser());

                response.put("success", true);
                response.put("message", "¡Cuenta verificada exitosamente! Ya puedes iniciar sesión.");
                return ResponseEntity.ok(response);
            }
        }
    }

}