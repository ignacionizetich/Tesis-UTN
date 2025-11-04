package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.NonAuthDTO.RegisterResponse;
import com.EDJ.ArCash.DTO.NonAuthDTO.RegistrerRequest;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;

@RestController
@RequestMapping(value = "/api/user", produces = "application/json")
@Tag(name = "Usuarios", description = "Operaciones relacionadas con usuarios")
public class UserController {

    @Autowired
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Registrar nuevo usuario",
            description = "Crea un nuevo usuario en el sistema."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario registrado correctamente",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"success\": false, \"message\": \"Datos inválidos\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"success\": false, \"message\": \"Error interno del servidor\"}"
                            )
                    )
            )
    })
    @PostMapping("/create")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegistrerRequest dto) throws MessagingException, UnsupportedEncodingException {

        if (dto.getName() == null || dto.getEmail() == null || dto.getPassword() == null || dto.getAlias() == null) {
            return ResponseEntity.badRequest().body(new RegisterResponse(false, "Todos los campos son obligatorios."));
        }
        
        try {
            User user = new User(dto.getName(), dto.getLastName(), dto.getDni(), dto.getEmail(), dto.getAlias());
            userService.insertarUsuario(user, dto.getPassword());
            return ResponseEntity.ok(new RegisterResponse(true, "Usuario registrado correctamente. Revisa tu email para activar tu cuenta."));
        } catch (RuntimeException e) {
            String message = e.getMessage();
            
            // Manejar múltiples errores
            if (message.contains(",")) {
                // Múltiples errores
                String[] errors = message.split(",");
                java.util.List<String> errorMessages = new java.util.ArrayList<>();
                
                for (String error : errors) {
                    switch (error.trim()) {
                        case "EMAIL_ALREADY_EXISTS":
                            errorMessages.add("el email ya se encuentra en uso");
                            break;
                        case "ALIAS_ALREADY_EXISTS":
                            errorMessages.add("el nombre de usuario no está disponible");
                            break;
                        case "DNI_ALREADY_EXISTS":
                            errorMessages.add("el DNI ya está registrado");
                            break;
                    }
                }
                
                String combinedMessage;
                if (errorMessages.size() == 1) {
                    combinedMessage = Character.toUpperCase(errorMessages.get(0).charAt(0)) + errorMessages.get(0).substring(1) + ".";
                } else if (errorMessages.size() == 2) {
                    combinedMessage = Character.toUpperCase(errorMessages.get(0).charAt(0)) + errorMessages.get(0).substring(1) + 
                                    " y " + errorMessages.get(1) + ".";
                } else {
                    String lastMessage = errorMessages.remove(errorMessages.size() - 1);
                    combinedMessage = Character.toUpperCase(errorMessages.get(0).charAt(0)) + errorMessages.get(0).substring(1);
                    for (int i = 1; i < errorMessages.size(); i++) {
                        combinedMessage += ", " + errorMessages.get(i);
                    }
                    combinedMessage += " y " + lastMessage + ".";
                }
                
                return ResponseEntity.badRequest().body(new RegisterResponse(false, combinedMessage));
            } else {
                // Error único
                switch (message) {
                    case "EMAIL_ALREADY_EXISTS":
                        return ResponseEntity.badRequest().body(new RegisterResponse(false, "El email ya se encuentra en uso."));
                    case "ALIAS_ALREADY_EXISTS":
                        return ResponseEntity.badRequest().body(new RegisterResponse(false, "El nombre de usuario no está disponible."));
                    case "DNI_ALREADY_EXISTS":
                        return ResponseEntity.badRequest().body(new RegisterResponse(false, "El DNI ya está registrado."));
                    default:
                        return ResponseEntity.status(500).body(new RegisterResponse(false, "Error interno del servidor."));
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new RegisterResponse(false, "Error interno del servidor."));
        }
    }
}