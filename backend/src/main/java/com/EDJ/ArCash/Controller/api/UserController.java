package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.NonAuthDTO.RegisterResponse;
import com.EDJ.ArCash.DTO.NonAuthDTO.RegistrerRequest;
import com.EDJ.ArCash.Service.result.RegisterResult;
import com.EDJ.ArCash.Service.interfaces.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/user", produces = "application/json")
@Tag(name = "Usuarios", description = "Operaciones relacionadas con usuarios")
public class UserController {

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
    public ResponseEntity<RegisterResponse> register(@RequestBody RegistrerRequest dto) {
        RegisterResult result = userService.registerFromRequest(dto);
        RegisterResponse body = result.toResponse();
        return switch (result.getKind()) {
            case OK -> ResponseEntity.ok(body);
            case VALIDATION, CONFLICT -> ResponseEntity.badRequest().body(body);
            case ERROR -> ResponseEntity.status(500).body(body);
        };
    }
}
