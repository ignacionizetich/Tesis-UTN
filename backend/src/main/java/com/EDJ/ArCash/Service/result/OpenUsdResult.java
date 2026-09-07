package com.EDJ.ArCash.Service.result;

import com.EDJ.ArCash.DTO.AuthDTO.OpenUsdAccountResponse;
import com.EDJ.ArCash.Models.Account;

/**
 * Alta de cuenta USD. El controller solo mapea Kind → HTTP.
 */
public final class OpenUsdResult {

    public enum Kind {
        OK,
        ALREADY_EXISTS,
        ERROR
    }

    private final Kind kind;
    private final String message;
    private final Long accountId;
    private final String accountAlias;

    private OpenUsdResult(Kind kind, String message, Long accountId, String accountAlias) {
        this.kind = kind;
        this.message = message;
        this.accountId = accountId;
        this.accountAlias = accountAlias;
    }

    public static OpenUsdResult ok(Account account) {
        return new OpenUsdResult(
                Kind.OK,
                "Cuenta en dólares creada exitosamente",
                account.getIdAccount(),
                account.getAccountNickname()
        );
    }

    public static OpenUsdResult alreadyExists() {
        return new OpenUsdResult(
                Kind.ALREADY_EXISTS,
                "El usuario ya cuenta con una cuenta en dolares",
                null,
                null
        );
    }

    public static OpenUsdResult error(String detail) {
        return new OpenUsdResult(
                Kind.ERROR,
                "Error al crear cuenta en dólares: " + detail,
                null,
                null
        );
    }

    public Kind getKind() {
        return kind;
    }

    public String getMessage() {
        return message;
    }

    public OpenUsdAccountResponse toSuccessBody() {
        return new OpenUsdAccountResponse(true, message, accountId, accountAlias, "USD");
    }

    public OpenUsdAccountResponse toErrorBody() {
        return new OpenUsdAccountResponse(false, message, null, null, null);
    }
}
