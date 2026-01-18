package com.EDJ.ArCash.factory;

import com.EDJ.ArCash.DTO.NonAuthDTO.RegisterResponse;
import org.springframework.stereotype.Component;

/**
 * Implementación del Factory para RegisterResponse
 * Maneja la creación de respuestas de registro de usuarios
 */
@Component
public class RegisterResponseFactory implements ResponseFactory<RegisterResponse> {

    @Override
    public RegisterResponse createSuccessResponse(String message) {
        return RegisterResponse.builder()
                .success(true)
                .mensaje(message)
                .build();
    }

    @Override
    public RegisterResponse createErrorResponse(String message) {
        return RegisterResponse.builder()
                .success(false)
                .mensaje(message)
                .build();
    }
}
