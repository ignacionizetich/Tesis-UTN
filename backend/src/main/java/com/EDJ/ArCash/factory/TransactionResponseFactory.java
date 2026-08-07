package com.EDJ.ArCash.factory;

import com.EDJ.ArCash.DTO.AuthDTO.TransactionResponse;
import org.springframework.stereotype.Component;

/**
 * Implementación del Factory para TransactionResponse
 * Maneja la creación de respuestas de transacciones
 */
@Component
public class TransactionResponseFactory implements ResponseFactory<TransactionResponse> {

    @Override
    public TransactionResponse createSuccessResponse(String message) {
        return TransactionResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    @Override
    public TransactionResponse createErrorResponse(String message) {
        return TransactionResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
