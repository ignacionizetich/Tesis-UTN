package com.EDJ.ArCash.Service.strategy;

import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.Imp.LogoutStatus;

/**
 * Interfaz Strategy para la gestión de tokens
 * Permite diferentes implementaciones de gestión de tokens (JWT, OAuth, etc.)
 */
public interface TokenManagementStrategy {
    
    /**
     * Genera un token de acceso para el usuario
     * 
     * @param userId ID del usuario
     * @param role Rol del usuario
     * @return Token de acceso generado
     */
    String generateAccessToken(String userId, String role);
    
    /**
     * Genera un token de actualización para el usuario
     * 
     * @param userId ID del usuario
     * @param role Rol del usuario
     * @return Token de actualización generado
     */
    String generateRefreshToken(String userId, String role);
    
    /**
     * Guarda el token de actualización en la base de datos
     * 
     * @param user Usuario asociado al token
     * @param refreshToken Token de actualización
     */
    void saveRefreshToken(User user, String refreshToken);
    
    /**
     * Revoca todos los tokens activos del usuario
     * 
     * @param accessToken Token de acceso del usuario
     * @return Estado del logout
     */
    LogoutStatus revokeUserTokens(String accessToken);
    
    /**
     * Obtiene el refresh token activo del usuario si existe
     * 
     * @param user Usuario
     * @return Refresh token activo o null si no existe
     */
    String getActiveRefreshToken(User user);
    
    /**
     * Extrae el ID de usuario del token
     * 
     * @param token Token
     * @return ID del usuario
     */
    String extractUserId(String token);

    /**
     * Indica si el access token corresponde a una sesion vigente
     * (refresh token activo para ese usuario).
     */
    boolean isValidSession(String accessToken);
}
