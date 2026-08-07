package com.EDJ.ArCash.factory;

import com.EDJ.ArCash.DTO.AuthDTO.LoginResponse;

/**
 * Interfaz especializada para la creación de LoginResponse
 * LoginResponse es único porque contiene tokens de autenticación, accountId y role
 */
public interface LoginResponseFactory {

    /**
     * Crea una respuesta de login exitosa completa
     * @param accessToken Token de acceso JWT
     * @param refreshToken Token de refresco JWT
     * @param accountId ID de la cuenta del usuario
     * @param role Rol/permiso del usuario
     * @return LoginResponse exitoso con todos los datos
     */
    LoginResponse createSuccessResponse(String accessToken, String refreshToken, Long accountId, String role);

    /**
     * Crea una respuesta de login con error
     * @param message Mensaje de error descriptivo
     * @return LoginResponse con error
     */
    LoginResponse createErrorResponse(String message);

    /**
     * Crea una respuesta de login por defecto (fallback)
     * @return LoginResponse con valores por defecto
     */
    LoginResponse createDefaultResponse();
}
