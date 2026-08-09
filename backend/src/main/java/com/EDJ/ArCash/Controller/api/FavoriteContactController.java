package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.AuthDTO.AddFavoriteContactRequest;
import com.EDJ.ArCash.DTO.AuthDTO.FavoriteContactResponse;
import com.EDJ.ArCash.DTO.AuthDTO.UpdateFavoriteContactRequest;
import com.EDJ.ArCash.Models.FavoriteContact;
import com.EDJ.ArCash.Security.JwtUtils;
import com.EDJ.ArCash.Service.FavoriteContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/favorites")
@Tag(name = "Contactos Favoritos", description = "Gestión de contactos favoritos")
public class FavoriteContactController {

    private final FavoriteContactService favoriteContactService;
    private final JwtUtils jwtUtils;

    public FavoriteContactController(FavoriteContactService favoriteContactService, JwtUtils jwtUtils) {
        this.favoriteContactService = favoriteContactService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/add")
    @Operation(summary = "Agregar contacto a favoritos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contacto agregado correctamente"),
            @ApiResponse(responseCode = "400", description = "Error en los datos proporcionados"),
            @ApiResponse(responseCode = "401", description = "Token no proporcionado o inválido")
    })
    public ResponseEntity<?> addFavoriteContact(
            @Valid @RequestBody AddFavoriteContactRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            Optional<Long> userIdOpt = extractUserIdFromRequest(httpRequest);
            if (userIdOpt.isEmpty()) {
                return createUnauthorizedResponse("Token no proporcionado o inválido");
            }

            Long userId = userIdOpt.get();
            boolean result = favoriteContactService.addFavoriteContact(
                    userId,
                    request.accountId(),
                    request.contactAlias().trim(),
                    request.description()
            );

            if (result) {
                return ResponseEntity.ok(Map.of(
                        "status", "SUCCESS",
                        "message", "Contacto agregado a favoritos correctamente"
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "status", "ERROR",
                                "message", "No se pudo agregar el contacto a favoritos"
                        ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Error interno del servidor");
        }
    }

    @GetMapping("/list")
    @Operation(summary = "Obtener contactos favoritos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente"),
            @ApiResponse(responseCode = "401", description = "Token no proporcionado o inválido")
    })
    public ResponseEntity<?> getFavoriteContacts(HttpServletRequest request) {
        try {
            Optional<Long> userIdOpt = extractUserIdFromRequest(request);
            if (userIdOpt.isEmpty()) {
                return createUnauthorizedResponse("Token no proporcionado o inválido");
            }

            Long userId = userIdOpt.get();
            List<FavoriteContact> favorites = favoriteContactService.getFavoriteContactsByUser(userId);

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "favorites", toResponse(favorites)
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Error interno del servidor");
        }
    }

    @GetMapping("/list/recent")
    @Operation(summary = "Obtener contactos favoritos ordenados por último uso")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente"),
            @ApiResponse(responseCode = "401", description = "Token no proporcionado o inválido")
    })
    public ResponseEntity<?> getFavoriteContactsOrderedByUsage(HttpServletRequest request) {
        try {
            Optional<Long> userIdOpt = extractUserIdFromRequest(request);
            if (userIdOpt.isEmpty()) {
                return createUnauthorizedResponse("Token no proporcionado o inválido");
            }

            Long userId = userIdOpt.get();
            List<FavoriteContact> favorites = favoriteContactService.getFavoriteContactsByUserOrderedByUsage(userId);

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "favorites", toResponse(favorites)
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Error interno del servidor");
        }
    }

    @DeleteMapping("/{favoriteId}")
    @Operation(summary = "Eliminar contacto favorito")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contacto eliminado correctamente"),
            @ApiResponse(responseCode = "400", description = "No se pudo eliminar el contacto"),
            @ApiResponse(responseCode = "401", description = "Token no proporcionado o inválido")
    })
    public ResponseEntity<?> removeFavoriteContact(
            @PathVariable Long favoriteId,
            HttpServletRequest request
    ) {
        try {
            Optional<Long> userIdOpt = extractUserIdFromRequest(request);
            if (userIdOpt.isEmpty()) {
                return createUnauthorizedResponse("Token no proporcionado o inválido");
            }

            Long userId = userIdOpt.get();
            boolean result = favoriteContactService.removeFavoriteContact(userId, favoriteId);

            if (result) {
                return ResponseEntity.ok(Map.of(
                        "status", "SUCCESS",
                        "message", "Contacto eliminado de favoritos correctamente"
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "status", "ERROR",
                                "message", "No se pudo eliminar el contacto favorito"
                        ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Error interno del servidor");
        }
    }

    @PutMapping("/update/{contactId}")
    @Operation(summary = "Actualizar contacto favorito", description = "Permite editar el alias y/o descripción de un contacto favorito")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contacto actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Contacto no encontrado"),
            @ApiResponse(responseCode = "498", description = "Token inválido")
    })
    public ResponseEntity<?> updateFavoriteContact(
            @Parameter(description = "ID del contacto favorito", required = true)
            @PathVariable Long contactId,
            @Valid @RequestBody UpdateFavoriteContactRequest request,
            HttpServletRequest httpRequest) {

        Optional<Long> userIdOpt = extractUserIdFromRequest(httpRequest);
        if (userIdOpt.isEmpty()) {
            return createUnauthorizedResponse("Token no proporcionado o inválido");
        }

        Long userId = userIdOpt.get();

        if ((request.contactAlias() == null || request.contactAlias().trim().isEmpty()) &&
                request.description() == null) {
            return ResponseEntity.status(400).body(Map.of(
                    "status", "ERROR",
                    "message", "Debe proporcionar al menos un campo para actualizar"
            ));
        }

        boolean success = favoriteContactService.updateFavoriteContact(
                contactId, userId, request.contactAlias(), request.description());

        if (success) {
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Contacto favorito actualizado correctamente"
            ));
        } else {
            return ResponseEntity.status(404).body(Map.of(
                    "status", "ERROR",
                    "message", "No se pudo actualizar el contacto. Verifique que existe y le pertenece."
            ));
        }
    }

    private List<FavoriteContactResponse> toResponse(List<FavoriteContact> favorites) {
        return favorites.stream()
                .map(FavoriteContactResponse::from)
                .toList();
    }

    private Optional<Long> extractUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }

        String token = authHeader.substring(7);
        String userIdStr = jwtUtils.extractUserId(token);
        if (userIdStr == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(userIdStr));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private ResponseEntity<?> createUnauthorizedResponse(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "status", "ERROR",
                        "message", message
                ));
    }

    private ResponseEntity<?> createErrorResponse(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", "ERROR",
                        "message", message
                ));
    }
}
