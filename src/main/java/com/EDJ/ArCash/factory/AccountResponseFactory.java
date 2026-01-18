package com.EDJ.ArCash.factory;

import com.EDJ.ArCash.DTO.AuthDTO.AccountResponse;
import org.springframework.stereotype.Component;

/**
 * Implementación del Factory para AccountResponse
 * Maneja la creación de respuestas de operaciones sobre cuentas
 */
@Component
public class AccountResponseFactory implements ResponseFactory<AccountResponse> {

    @Override
    public AccountResponse createSuccessResponse(String message) {
        return AccountResponse.builder()
                .success(true)
                .message(message)
                .newBalance(0.0)
                .build();
    }

    /**
     * Crea una respuesta exitosa con el nuevo balance
     * @param message Mensaje descriptivo
     * @param newBalance Nuevo saldo de la cuenta
     * @return AccountResponse exitoso con balance
     */
    public AccountResponse createSuccessResponse(String message, double newBalance) {
        return AccountResponse.builder()
                .success(true)
                .message(message)
                .newBalance(newBalance)
                .build();
    }

    @Override
    public AccountResponse createErrorResponse(String message) {
        return AccountResponse.builder()
                .success(false)
                .message(message)
                .newBalance(0.0)
                .build();
    }
}
