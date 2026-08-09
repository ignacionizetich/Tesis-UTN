package com.EDJ.ArCash.DTO.AuthDTO;

import com.EDJ.ArCash.Models.Account;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Listado de las cuentas del usuario autenticado")
public record UserAccountsResponse(
        @Schema(description = "Indica si la consulta fue exitosa", example = "true")
        boolean success,

        @Schema(description = "Cuentas del usuario, en cualquiera de sus monedas")
        List<UserAccountResponse> accounts
) {
    public static UserAccountsResponse from(List<Account> accounts) {
        return new UserAccountsResponse(
                true,
                accounts.stream().map(UserAccountResponse::from).toList()
        );
    }
}
