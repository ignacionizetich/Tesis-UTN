package com.EDJ.ArCash.Controller.api;


import com.EDJ.ArCash.DTO.AuthDTO.AdminRequest;
import com.EDJ.ArCash.DTO.AuthDTO.UserResponse;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.ValidationToken;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.Service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping(value = "/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class ApiAdminController {


    @Autowired
    private AdminService adminService;

    @Autowired
    private PasswordEncoder passwordEncoder;





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
    public ResponseEntity<List<UserResponse>> getAllAuthenticatedUsers(HttpServletRequest request) {
        adminService.validarAdmin(request);

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
    public ResponseEntity<?> disableUser(@PathVariable Long id, HttpServletRequest request) {
        adminService.validarAdmin(request);
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
    public ResponseEntity<?> enableUser(@PathVariable Long id, HttpServletRequest request) {
        adminService.validarAdmin(request);
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
    public ResponseEntity<?> createAdminUser(@RequestBody AdminRequest adminRequest,HttpServletRequest request)  {
        adminService.validarAdmin(request);
        User user = new User();
        user.setName(adminRequest.getName().substring(0,1).toUpperCase() + adminRequest.getName().substring(1).toLowerCase());
        user.setLastName(adminRequest.getLastName().substring(0,1).toUpperCase() + adminRequest.getLastName().substring(1).toLowerCase());
        user.setPermissions(Permissions.ADMIN);
        user.setDni(adminRequest.getDni());
        user.setEmail(adminRequest.getEmail());
        user.setAlias(adminRequest.getUsername());
        user.setEnabled(true);
        user.setActive(true);
        // Crear credenciales y token en cascada
        user.setCredentials(new Credentials(user, user.getAlias(), passwordEncoder.encode(adminRequest.getPassword())));

        adminService.cargarAdmin(user);
        return ResponseEntity.ok("Usuario administrador creado correctamente");

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




