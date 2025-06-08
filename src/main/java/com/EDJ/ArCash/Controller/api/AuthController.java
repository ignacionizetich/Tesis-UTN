package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.LoginRequest;
import com.EDJ.ArCash.DTO.LoginResponse;

import com.EDJ.ArCash.Models.Imp.LogoutStatus;
import com.EDJ.ArCash.Security.JwtUtils;
import com.EDJ.ArCash.Service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;


import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping(value = "/api/auth", produces = "application/json")
public class AuthController {



    @Autowired
    private AuthService authService;

    @Operation(description = "Este endpoint maneja la logica de log-in de los usuarios a nuestra aplicacion")
    @Parameter(description = "Recibe por parametro un body JSON y usamos un loginRequest que es un DTO para verificar las credenciales del usuario y verificar si son correctas")
    @ApiResponse(description = "202 OK")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            LoginResponse loginResponse = authService.login(loginRequest);

            if (loginResponse.isSuccess()) {
                String refreshToken = loginResponse.getRefreshToken();
                if (refreshToken != null) {
                    ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                            .httpOnly(true)
                            .path("/")
                            .maxAge(7 * 24 * 60 * 60)
                            .build();
                    // Agregar la cookie correctamente al header
                    response.addHeader("Set-Cookie", cookie.toString());
                }
                return ResponseEntity.ok(loginResponse);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(loginResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new LoginResponse(false, "Error interno del servidor", null, null, null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        LogoutStatus status = authService.logout(token);

        // Borrar la cookie del refresh token
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .path("/")
                .httpOnly(true)
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(status);
    }

    @GetMapping("/check-session")
    public ResponseEntity<?> checkSession(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                // Verificar si el token es válido y no está revocado
                boolean isValidSession = authService.isValidSession(token);

                if (isValidSession) {
                    return ResponseEntity.ok()
                            .body(Map.of(
                                    "status", "ACTIVE",
                                    "message", "Sesión activa"
                            ));
                }
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "status", "INACTIVE",
                            "message", "No hay sesión activa"
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Error al verificar la sesión"
                    ));
        }
    }

    @PostMapping("/send-recover-mail")
    public ResponseEntity<?> sendRecoverEmail(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            boolean enviado = authService.enviarCorreoRecuperacion(email);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace(); // Esto mostrará el error en la consola
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno al enviar el correo");
        }
    }



    @PutMapping("/changeUsername")
    public ResponseEntity<?> changeUsername(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String newUsername = body.get("newUsername");
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "El nombre de usuario no puede estar vacío."));
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Token no proporcionado."));
        }
        String token = authHeader.substring(7);

        // Extraer userId del token
        String userIdStr = JwtUtils.extractUserId(token);
        if (userIdStr == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Token inválido."));
        }

        boolean result = authService.cambiarAliasYUsername(Long.parseLong(userIdStr), newUsername.trim());
        if (result) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Nombre de usuario actualizado correctamente."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "No se pudo actualizar el nombre de usuario. Puede que ya exista."));
        }
    }



}