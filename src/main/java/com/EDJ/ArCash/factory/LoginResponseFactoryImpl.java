package com.EDJ.ArCash.factory;

import com.EDJ.ArCash.DTO.AuthDTO.LoginResponse;
import org.springframework.stereotype.Component;

/**
 * Implementación del Factory para LoginResponse
 * Maneja la creación de respuestas de autenticación (login)
 */
@Component
public class LoginResponseFactoryImpl implements LoginResponseFactory {

    @Override
    public LoginResponse createSuccessResponse(String accessToken, String refreshToken, Long accountId, String role) {
        return LoginResponse.builder()
                .success(true)
                .message("Inicio de sesión exitoso!")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accountId(accountId)
                .role(role)
                .build();
    }

    @Override
    public LoginResponse createErrorResponse(String message) {
        return LoginResponse.builder()
                .success(false)
                .message(message)
                .accessToken(null)
                .refreshToken(null)
                .accountId(null)
                .role(null)
                .build();
    }

    @Override
    public LoginResponse createDefaultResponse() {
        return LoginResponse.builder()
                .success(true)
                .message("Respuesta por defecto")
                .build();
    }
}



