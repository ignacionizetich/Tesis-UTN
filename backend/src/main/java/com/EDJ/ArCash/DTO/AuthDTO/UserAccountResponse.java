package com.EDJ.ArCash.DTO.AuthDTO;

import com.EDJ.ArCash.Models.Account;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos de una cuenta del usuario")
public record UserAccountResponse(
        @Schema(description = "Identificador de la cuenta", example = "10")
        String id,

        @Schema(description = "Saldo actual", example = "1500.75")
        double balance,

        @Schema(description = "Alias de la cuenta", example = "MI.CUENTA.AA")
        String alias,

        @Schema(description = "CVU de la cuenta", example = "0000200112345678901234")
        String cvu,

        @Schema(description = "Moneda de la cuenta", example = "ARS")
        String currency
) {
    public static UserAccountResponse from(Account account) {
        return new UserAccountResponse(
                String.valueOf(account.getIdAccount()),
                account.getBalance(),
                account.getAccountNickname(),
                account.getAccountCvu(),
                account.getAccountType().toString()
        );
    }
}
