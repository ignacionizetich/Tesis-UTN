package com.EDJ.ArCash.Service.strategy;

import com.EDJ.ArCash.Models.User;

/**
 * Resultado de validar credenciales, sin emitir tokens ni resolver la cuenta.
 * Permite que AuthenticationStrategy no conozca TokenManagementStrategy.
 */
public final class AuthenticationResult {

    private final boolean success;
    private final String errorMessage;
    private final User user;

    private AuthenticationResult(boolean success, String errorMessage, User user) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.user = user;
    }

    public static AuthenticationResult failure(String errorMessage) {
        return new AuthenticationResult(false, errorMessage, null);
    }

    public static AuthenticationResult success(User user) {
        return new AuthenticationResult(true, null, user);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public User getUser() {
        return user;
    }
}
