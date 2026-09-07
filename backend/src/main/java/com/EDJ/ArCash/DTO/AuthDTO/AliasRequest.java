package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Nuevo alias de una cuenta (el identificador con puntos que se usa para recibir
 * transferencias, distinto del nombre de usuario).
 *
 * <p>El formato exacto lo sigue verificando {@code AliasFormatValidator} en el servicio, que es
 * quien conoce la regla de negocio y ya devuelve un resultado tipado; aca solo se corta lo
 * estructuralmente invalido antes de llegar a la capa de dominio.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para cambiar el alias de la cuenta")
public class AliasRequest {

    @NotBlank(message = "El nuevo alias es obligatorio")
    @Size(min = 4, max = 25, message = "El alias debe tener entre 4 y 25 caracteres")
    @Schema(description = "Nuevo alias para la cuenta", example = "mi.nuevo.alias")
    private String newAlias;
}
