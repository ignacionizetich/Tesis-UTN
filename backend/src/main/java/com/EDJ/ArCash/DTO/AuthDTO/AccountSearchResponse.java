package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Resultado de búsqueda de cuenta por alias o CVU")
public class AccountSearchResponse {

    @Schema(description = "ID de la cuenta", example = "10")
    private Long idaccount;

    @Schema(description = "Alias de la cuenta", example = "juan.perez.aa")
    private String alias;

    @Schema(description = "CVU de la cuenta", example = "0000003100010000000001")
    private String cvu;

    @Schema(description = "Moneda de la cuenta", example = "ARS")
    private String currency;

    @Schema(description = "Datos del titular")
    private UserSummary user;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserSummary {
        private String nombre;
        private String apellido;
        private String dni;
    }
}
