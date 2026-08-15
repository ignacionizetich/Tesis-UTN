package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.AuthDTO.LoginRequest;
import com.EDJ.ArCash.DTO.AuthDTO.LoginResponse;
import com.EDJ.ArCash.DTO.AuthDTO.UsernameRequest;
import com.EDJ.ArCash.DTO.AuthDTO.UsernameResponse;
import com.EDJ.ArCash.Models.Imp.LogoutStatus;
import com.EDJ.ArCash.Security.CustomUserDetails;
import com.EDJ.ArCash.Service.interfaces.AuthService;
import com.EDJ.ArCash.Service.result.RecoverMailResult;
import com.EDJ.ArCash.Service.result.RefreshAccessResult;
import com.EDJ.ArCash.Service.result.SessionCheckResult;
import com.EDJ.ArCash.Service.interfaces.UserService;
import com.EDJ.ArCash.Service.result.UsernameChangeResult;
import com.EDJ.ArCash.Service.strategy.AuthenticationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/auth", produces = "application/json")
@Tag(name = "Usuarios Autenticados", description = "Operaciones para usuarios autenticados")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Operation(
            summary = "Iniciar sesión",
            description = "Verifica las credenciales del usuario y retorna tokens de acceso y refresh si son válidas."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Inicio de sesión exitoso",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciales inválidas",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            )
    })
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
            response.addHeader("Set-Cookie", cookie.toString());
          }
          return ResponseEntity.ok(loginResponse);
        }

        HttpStatus status = AuthenticationResult.USER_DISABLED_MESSAGE.equals(loginResponse.getMessage())
          ? HttpStatus.FORBIDDEN
          : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(loginResponse);

      } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(new LoginResponse(false, "Error interno del servidor", null, null, null, null));
      }
    }

    @Operation(
            summary = "Cerrar sesión",
            description = "Cierra la sesión del usuario y elimina el refresh token."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Sesión cerrada correctamente",
                    content = @Content(schema = @Schema(implementation = LogoutStatus.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token inválido o no proporcionado",
                    content = @Content(schema = @Schema(implementation = LogoutStatus.class))
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpServletResponse response,
            @Parameter(description = "Token JWT de autenticación", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        LogoutStatus status = authService.logout(token);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .path("/")
                .httpOnly(true)
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(status);
    }

    @Operation(
            summary = "Verificar sesión",
            description = "Verifica si el token JWT enviado es válido y la sesión está activa."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Sesión activa",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"status\": \"ACTIVE\", \"message\": \"Sesión activa\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Sesión inactiva o token inválido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"status\": \"INACTIVE\", \"message\": \"No hay sesión activa\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"status\": \"ERROR\", \"message\": \"Error al verificar la sesión\"}"
                            )
                    )
            )
    })
    @GetMapping("/check-session")
    public ResponseEntity<?> checkSession(
            @Parameter(description = "Token JWT de autenticación", example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SessionCheckResult result = authService.checkSession(authHeader);
        return switch (result.getKind()) {
            case ACTIVE -> ResponseEntity.ok(result.toBody());
            case INACTIVE -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result.toBody());
            case ERROR -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result.toBody());
        };
    }

    @Operation(
            summary = "Enviar correo de recuperación",
            description = "Envía un correo electrónico para recuperar la contraseña del usuario."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Correo enviado correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"message\": \"Correo enviado correctamente\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno al enviar el correo",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"message\": \"Error interno al enviar el correo\"}"
                            )
                    )
            )
    })
    @PostMapping("/send-recover-mail")
    public ResponseEntity<?> sendRecoverEmail(@RequestBody Map<String, String> body) {
        RecoverMailResult result = authService.sendRecoverMail(body.get("email"));
        return switch (result.getKind()) {
            case OK -> ResponseEntity.ok(result.toBody());
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(result.toBody());
            case ERROR -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result.toBody());
        };
    }

    @Operation(
            summary = "Cambiar nombre de usuario",
            description = "Permite al usuario autenticado cambiar su nombre de usuario."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Nombre de usuario actualizado correctamente",
                    content = @Content(schema = @Schema(implementation = UsernameResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = UsernameResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token no proporcionado o inválido",
                    content = @Content(schema = @Schema(implementation = UsernameResponse.class))
            )
    })
    @PutMapping("/changeUsername")
    public ResponseEntity<?> changeUsername(
            @RequestBody UsernameRequest usernameRequest,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        UsernameChangeResult result = userService.changeUsername(
                principal.getUser().getId(), usernameRequest.getNewUsername());
        UsernameResponse body = new UsernameResponse(result.isSuccess(), result.getMessage());
        return switch (result.getKind()) {
            case OK -> ResponseEntity.ok(body);
            case EMPTY, FAIL -> ResponseEntity.badRequest().body(body);
        };
    }

    @Operation(
            summary = "Refrescar token de acceso",
            description = "Genera un nuevo token de acceso JWT usando un refresh token válido enviado en la cookie."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Nuevo token de acceso generado correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"accessToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Refresh token requerido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"error\": \"Refresh token requerido\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh token inválido o expirado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "{\"error\": \"Refresh token inválido o expirado\"}"
                            )
                    )
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        RefreshAccessResult result = authService.refreshAccessToken(refreshToken);
        return switch (result.getKind()) {
            case MISSING -> ResponseEntity.badRequest().body(Map.of("error", result.getError()));
            case INVALID -> ResponseEntity.status(401).body(Map.of("error", result.getError()));
            case OK -> ResponseEntity.ok(Map.of("accessToken", result.getAccessToken()));
        };
    }

}
