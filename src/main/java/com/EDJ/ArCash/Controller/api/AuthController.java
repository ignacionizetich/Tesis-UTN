package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.LoginRequest;
import com.EDJ.ArCash.DTO.LoginResponse;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.CredentialRepository;
import com.EDJ.ArCash.Security.JwtUtils;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;


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


}





