package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.LoginRequest;
import com.EDJ.ArCash.DTO.LoginResponse;
import com.EDJ.ArCash.DTO.LogoutResponse;
import com.EDJ.ArCash.Models.Imp.LogoutStatus;
import com.EDJ.ArCash.Repository.CredentialRepository;
import com.EDJ.ArCash.Service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping(value = "/api/auth", produces = "application/json")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;



    @Autowired
    private AuthService authService;

    @Operation(description = "Este endpoint maneja la logica de log-in de los usuarios a nuestra aplicacion")
    @Parameter(description = "Recibe por parametro un body JSON y usamos un loginRequest que es un DTO para verificar las credenciales del usuario y verificar si son correctas")
    @ApiResponse(description = "202 OK")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            LoginResponse loginResponse = authService.login(loginRequest);

            // Si el login es exitoso
            if (loginResponse.isSuccess()) {
                // Generar el refresh token y agregarlo en una cookie HttpOnly
                String refreshToken = loginResponse.getRefreshToken();
                if (refreshToken != null) {
                    // Crear la cookie con el refresh token
                    Cookie cookie = new Cookie("refreshToken", refreshToken);
                    cookie.setHttpOnly(true); // No accesible desde JavaScript
                    cookie.setSecure(true);   // Solo se envía por HTTPS
                    cookie.setPath("/");      // Disponible en todo el dominio
                    cookie.setMaxAge(7 * 24 * 3600); // Establecer el tiempo de expiración de la cookie (7 días)

                    // Agregar la cookie a la respuesta
                    response.addCookie(cookie);
                }


                // Devolver el access token al cliente
                return ResponseEntity.ok(loginResponse);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(loginResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new LoginResponse(false, "Error interno del servidor",null,null,null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(
            @RequestHeader(value = "Authorization") String authHeader,
            HttpServletResponse response) {
        try {
            // Extraer el access token del header de autorización
            String accessToken = authHeader.substring(7);

            // Llamar al método de logout con solo el access token
            LogoutStatus status = authService.logout(accessToken);

            switch (status) {
                case SUCCESS:
                    // Eliminar la cookie del refresh token
                    Cookie cookie = new Cookie("refreshToken", null);
                    cookie.setHttpOnly(true);
                    cookie.setSecure(true);
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);

                    return ResponseEntity.ok()
                            .body(new LogoutResponse(true, "Sesión cerrada correctamente."));

                case ALREADY_REVOKED:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new LogoutResponse(false, "El token ya ha sido revocado previamente"));

                case ERROR:
                default:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new LogoutResponse(false, "Error al cerrar sesión."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new LogoutResponse(false, "Error al procesar la solicitud"));
        }
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


}