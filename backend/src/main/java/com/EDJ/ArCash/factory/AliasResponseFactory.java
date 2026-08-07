package com.EDJ.ArCash.factory;

import com.EDJ.ArCash.DTO.AuthDTO.AliasResponse;
import org.springframework.stereotype.Component;

/**
 * Implementación del Factory para AliasResponse
 * Maneja la creación de respuestas de operaciones sobre alias de cuenta
 */
@Component
public class AliasResponseFactory implements ResponseFactory<AliasResponse> {

    @Override
    public AliasResponse createSuccessResponse(String message) {
        return AliasResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    @Override
    public AliasResponse createErrorResponse(String message) {
        return AliasResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
