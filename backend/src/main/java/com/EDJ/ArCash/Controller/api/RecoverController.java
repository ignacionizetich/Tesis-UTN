package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.AuthDTO.RecoveryTokenValidationResponse;
import com.EDJ.ArCash.DTO.common.ApiMessageResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.EDJ.ArCash.Service.interfaces.AuthService;
import com.EDJ.ArCash.Service.interfaces.CredentialsService;
import com.EDJ.ArCash.Service.result.RecoveryTokenValidationResult;
import com.EDJ.ArCash.Service.result.ResetPasswordResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/auth")
public class RecoverController {

    private final AuthService authService;
    private final CredentialsService credentialsService;

    public RecoverController(AuthService authService, CredentialsService credentialsService) {
        this.authService = authService;
        this.credentialsService = credentialsService;
    }

    @Operation(
            summary = "Validar token de recuperación",
            description = "Valida si un token de recuperación es válido y no ha sido usado."
    )
    @GetMapping("/validate-recovery-token")
    public ResponseEntity<RecoveryTokenValidationResponse> validateRecoveryToken(
            @RequestParam("token") String token) {
        RecoveryTokenValidationResult result = authService.validateRecoveryToken(token);
        return switch (result.getKind()) {
            case VALID -> ResponseEntity.ok(result.toBody());
            case INVALID -> ResponseEntity.status(401).body(result.toBody());
            case ERROR -> ResponseEntity.status(500).body(result.toBody());
        };
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
    public ResponseEntity<ApiMessageResponse> resetPassword(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword) {

        // Sin try/catch: un fallo inesperado lo traduce GlobalExceptionHandler, que ademas
        // deja el stack trace y el traceId en el log en lugar de tragarse la causa.
        ResetPasswordResult resultado =
                credentialsService.actualizarPassword(token, password, confirmPassword);
        ApiMessageResponse body =
                new ApiMessageResponse(resultado.isSuccess(), resultado.getMessage());

        return switch (resultado.getKind()) {
            case OK -> ResponseEntity.ok(body);
            case UNAUTHORIZED -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            case BAD_REQUEST -> ResponseEntity.badRequest().body(body);
        };
    }
}
