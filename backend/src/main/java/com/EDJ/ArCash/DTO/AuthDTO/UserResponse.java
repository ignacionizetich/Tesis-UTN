package com.EDJ.ArCash.DTO.AuthDTO;

import com.EDJ.ArCash.Models.Imp.Permissions;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta con información de usuario")
public class UserResponse {
  @Schema(description = "ID del usuario", example = "1")
  private long id;

  @Schema(description = "Nombre del usuario", example = "Juan")
  private String name;

  @Schema(description = "Apellido del usuario", example = "Pérez")
  private String lastName;

  @Schema(description = "DNI del usuario", example = "12345678")
  private String dni;

  @Schema(description = "Correo electrónico", example = "juan.perez@email.com")
  private String email;

  @Schema(description = "Nombre de usuario", example = "juanp")
  private String username;

  @Schema(description = "ID de la cuenta asociada", example = "10")
  private Long idAccount;

  @Schema(description = "Indica si el usuario está habilitado", example = "true")
  private boolean enabled;

  @Schema(description = "Indica si el usuario está activo", example = "true")
  private boolean active;

  @Schema(description = "Nivel de permisos del usuario", example = "ADMIN")
  private Permissions permissions;

  @Schema(description = "Fecha de alta del usuario", example = "2026-01-15 10:30:00")
  private String creationDate;
}
