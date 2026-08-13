package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.AuthDTO.AddFavoriteContactRequest;
import com.EDJ.ArCash.DTO.AuthDTO.FavoriteContactResponse;
import com.EDJ.ArCash.DTO.AuthDTO.UpdateFavoriteContactRequest;
import com.EDJ.ArCash.Models.FavoriteContact;
import com.EDJ.ArCash.Security.CustomUserDetails;
import com.EDJ.ArCash.Service.interfaces.FavoriteContactService;
import com.EDJ.ArCash.Service.result.FavoriteUpdateResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@Tag(name = "Contactos Favoritos", description = "Gestión de contactos favoritos")
public class FavoriteContactController {

    private final FavoriteContactService favoriteContactService;

    public FavoriteContactController(FavoriteContactService favoriteContactService) {
        this.favoriteContactService = favoriteContactService;
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
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        // Inalcanzable en produccion: SecurityConfig.anyRequest().authenticated().
        // Se preserva para tests con addFilters=false (mismo criterio Fase 4).
        if (principal == null) {
            return createUnauthorizedResponse("Token no proporcionado o inválido");
        }

        Long userId = principal.getUser().getId();
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
        }

        return ResponseEntity.badRequest()
                .body(Map.of(
                        "status", "ERROR",
                        "message", "No se pudo agregar el contacto a favoritos"
                ));
    }

    @GetMapping("/list")
    @Operation(summary = "Obtener contactos favoritos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente"),
            @ApiResponse(responseCode = "401", description = "Token no proporcionado o inválido")
    })
    public ResponseEntity<?> getFavoriteContacts(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return createUnauthorizedResponse("Token no proporcionado o inválido");
        }

        Long userId = principal.getUser().getId();
        List<FavoriteContact> favorites = favoriteContactService.getFavoriteContactsByUser(userId);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "favorites", toResponse(favorites)
        ));
    }

    @GetMapping("/list/recent")
    @Operation(summary = "Obtener contactos favoritos ordenados por último uso")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente"),
            @ApiResponse(responseCode = "401", description = "Token no proporcionado o inválido")
    })
    public ResponseEntity<?> getFavoriteContactsOrderedByUsage(
            @AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) {
            return createUnauthorizedResponse("Token no proporcionado o inválido");
        }

        Long userId = principal.getUser().getId();
        List<FavoriteContact> favorites = favoriteContactService.getFavoriteContactsByUserOrderedByUsage(userId);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "favorites", toResponse(favorites)
        ));
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
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        if (principal == null) {
            return createUnauthorizedResponse("Token no proporcionado o inválido");
        }

        Long userId = principal.getUser().getId();
        boolean result = favoriteContactService.removeFavoriteContact(userId, favoriteId);

        if (result) {
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Contacto eliminado de favoritos correctamente"
            ));
        }

        return ResponseEntity.badRequest()
                .body(Map.of(
                        "status", "ERROR",
                        "message", "No se pudo eliminar el contacto favorito"
                ));
    }

    @PutMapping("/update/{contactId}")
    @Operation(summary = "Actualizar contacto favorito", description = "Permite editar el alias y/o descripción de un contacto favorito")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contacto actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Contacto no encontrado"),
            @ApiResponse(responseCode = "401", description = "Token no proporcionado o inválido")
    })
    public ResponseEntity<?> updateFavoriteContact(
            @Parameter(description = "ID del contacto favorito", required = true)
            @PathVariable Long contactId,
            @Valid @RequestBody UpdateFavoriteContactRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (principal == null) {
            return createUnauthorizedResponse("Token no proporcionado o inválido");
        }

        FavoriteUpdateResult result = favoriteContactService.updateFavoriteContactForOwner(
                contactId, principal.getUser().getId(), request.contactAlias(), request.description());

        return switch (result.getKind()) {
            case OK -> ResponseEntity.ok(result.toBody("SUCCESS"));
            case BAD_REQUEST -> ResponseEntity.status(400).body(result.toBody("ERROR"));
            case NOT_FOUND -> ResponseEntity.status(404).body(result.toBody("ERROR"));
        };
    }

    private List<FavoriteContactResponse> toResponse(List<FavoriteContact> favorites) {
        return favorites.stream()
                .map(FavoriteContactResponse::from)
                .toList();
    }

    private ResponseEntity<?> createUnauthorizedResponse(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "status", "ERROR",
                        "message", message
                ));
    }
}
