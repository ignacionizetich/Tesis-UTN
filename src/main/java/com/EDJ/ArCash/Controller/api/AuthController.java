package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.LoginRequest;
import com.EDJ.ArCash.DTO.LoginResponse;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.CredentialRepository;
import com.EDJ.ArCash.Security.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Operation(description = "Este endpoint maneja la logica de log-in de los usuarios a nuestra aplicacion")
    @Parameter(description = "Recibe por parametro un body JSON y usamos un loginRequest que es un DTO para verificar las credenciales del usuario y verificar si son correctas")
    @ApiResponse(description = "202 OK")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Optional<Credentials> credentialsOptional = credentialRepository.findByUsername(loginRequest.getUsername());

            if (credentialsOptional.isPresent()) {
                Credentials credentials = credentialsOptional.get();
                User usuario = credentials.getUser();

                if (passwordEncoder.matches(loginRequest.getPassword(), credentials.getPass())) {
                    if(!usuario.isEnabled()) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new LoginResponse(false, "Usuario no habilitado", null));
                    }
                    String token = JwtUtils.generateToken(credentials.getUsername());
                    return ResponseEntity.ok(new LoginResponse(true, "Login exitoso", token));
                }
                else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new LoginResponse(false, "Credenciales incorrectas", null));
                }
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new LoginResponse(false, "Usuario no encontrado", null));

        } catch (Exception e) {
            e.printStackTrace(); // <-- Mostrará el error real en consola
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new LoginResponse(false, "Error interno del servidor", null));
        }
    }


}





