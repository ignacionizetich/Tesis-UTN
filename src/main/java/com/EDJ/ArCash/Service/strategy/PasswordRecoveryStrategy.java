package com.EDJ.ArCash.Service.strategy;

/**
 * Interfaz Strategy para la recuperación de contraseñas
 * Permite implementar diferentes métodos de recuperación (email, SMS, etc.)
 */
public interface PasswordRecoveryStrategy {
    
    /**
     * Envía un correo de recuperación al usuario
     * 
     * @param email Email del usuario
     * @return true si se envió correctamente, false en caso contrario
     */
    boolean sendRecoveryEmail(String email);
    
    /**
     * Valida si un token de recuperación es válido
     * 
     * @param token Token de recuperación
     * @return true si el token es válido, false en caso contrario
     */
    boolean validateRecoveryToken(String token);
    
    /**
     * Reenvía el enlace de recuperación
     * 
     * @param email Email del usuario
     * @return true si se reenvió correctamente, false en caso contrario
     */
    boolean resendRecoveryLink(String email);
}
