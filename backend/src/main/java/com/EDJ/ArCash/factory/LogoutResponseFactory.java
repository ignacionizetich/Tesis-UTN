package com.EDJ.ArCash.factory;

import com.EDJ.ArCash.DTO.AuthDTO.LogoutResponse;
import org.springframework.stereotype.Component;

/**
 * Implementación del Factory para LogoutResponse
 * Maneja la creación de respuestas de cierre de sesión
 */
@Component
public class LogoutResponseFactory implements ResponseFactory<LogoutResponse> {

    @Override
    public LogoutResponse createSuccessResponse(String message) {
        return LogoutResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    @Override
    public LogoutResponse createErrorResponse(String message) {
        return LogoutResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
