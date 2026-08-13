package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.AuthDTO.AdminMetricsResponse;
import com.EDJ.ArCash.DTO.AuthDTO.AdminRequest;
import com.EDJ.ArCash.DTO.AuthDTO.LoanRatesResponse;
import com.EDJ.ArCash.DTO.AuthDTO.LoanRatesUpdateRequest;
import com.EDJ.ArCash.DTO.AuthDTO.UserResponse;
import com.EDJ.ArCash.Service.result.AdminCreateResult;
import com.EDJ.ArCash.Service.interfaces.AdminService;
import com.EDJ.ArCash.Service.interfaces.LoanRateConfigService;
import com.EDJ.ArCash.Service.interfaces.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class ApiAdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private LoanRateConfigService loanRateConfigService;

    @Operation(
            summary = "Métricas del sistema",
            description = "Devuelve KPIs y series para el panel de administración."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Métricas generadas"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @GetMapping("/metrics")
    public ResponseEntity<AdminMetricsResponse> getMetrics() {
        return ResponseEntity.ok(metricsService.collect());
    }

    @Operation(
            summary = "Obtener tasas de préstamos",
            description = "Devuelve las tasas mensuales configuradas por cantidad de cuotas."
    )
    @GetMapping("/loan-rates")
    public ResponseEntity<LoanRatesResponse> getLoanRates() {
        return ResponseEntity.ok(loanRateConfigService.listRates());
    }

    @Operation(
            summary = "Actualizar tasas de préstamos",
            description = "Actualiza las tasas mensuales por plazo (3, 6 y 12 cuotas)."
    )
    @PutMapping("/loan-rates")
    public ResponseEntity<?> updateLoanRates(@RequestBody LoanRatesUpdateRequest request) {
        try {
            return ResponseEntity.ok(loanRateConfigService.updateRates(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", ex.getMessage()));
        }
    }

    @Operation(
            summary = "Obtener todos los usuarios autenticados",
            description = "Devuelve una lista de usuarios autenticados en el sistema."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de usuarios autenticados",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado"
            )
    })
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllAuthenticatedUsers() {
        return ResponseEntity.ok(adminService.getAuthUsers());
    }

    @Operation(
            summary = "Deshabilitar usuario",
            description = "Deshabilita un usuario por su ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario deshabilitado correctamente"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado"
            )
    })
    @PutMapping("/users/{id}/disable")
    public ResponseEntity<?> disableUser(@PathVariable Long id) {
        adminService.disableUser(id);
        return ResponseEntity.ok("usuario deshabilitado correctamente");
    }

    @Operation(
            summary = "Habilitar usuario",
            description = "Habilita un usuario por su ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario habilitado correctamente"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado"
            )
    })
    @PutMapping("/users/{id}/enable")
    public ResponseEntity<?> enableUser(@PathVariable Long id) {
        adminService.enableUser(id);
        return ResponseEntity.ok("usuario habilitado correctamente");
    }

    @Operation(
            summary = "Crear usuario administrador",
            description = "Crea un nuevo usuario con permisos de administrador."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario administrador creado correctamente"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado"
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del nuevo administrador",
            required = true,
            content = @Content(schema = @Schema(implementation = AdminRequest.class))
    )
    @PostMapping("/users/create-admin")
    public ResponseEntity<?> createAdminUser(@RequestBody AdminRequest adminRequest) {
        AdminCreateResult resultado = adminService.createAdmin(adminRequest);

        return switch (resultado.getKind()) {
            case SUCCESS -> ResponseEntity.ok("Usuario administrador creado correctamente");
            case CONFLICT -> {
                Map<String, String> body = new HashMap<>();
                body.put("mensaje", resultado.getMensaje());
                if (resultado.getCampo() != null) {
                    body.put("campo", resultado.getCampo());
                }
                yield ResponseEntity.status(409).body(body);
            }
            case ERROR -> ResponseEntity.status(500).body(Map.of(
                    "mensaje", resultado.getMensaje()
            ));
        };
    }

    @Operation(
            summary = "Verificar acceso de administrador",
            description = "Verifica si el usuario autenticado tiene acceso de administrador."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Acceso de administrador verificado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado"
            )
    })
    @GetMapping("/check-access")
    public ResponseEntity<?> checkAdminAccess() {
        return ResponseEntity.ok().build();
    }
}
