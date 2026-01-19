package com.EDJ.ArCash.Service.strategy;

import com.EDJ.ArCash.DTO.AuthDTO.LoginRequest;
import com.EDJ.ArCash.DTO.AuthDTO.LoginResponse;

/**
 * Interfaz Strategy para diferentes estrategias de autenticación
 * Permite implementar múltiples métodos de autenticación (básica, OAuth, 2FA, etc.)
 * cumpliendo con el Principio Open/Closed
 */
public interface AuthenticationStrategy {
    
    /**
     * Autentica a un usuario con las credenciales proporcionadas
     * 
     * @param loginRequest Solicitud de login con credenciales
     * @return LoginResponse con el resultado de la autenticación
     */
    LoginResponse authenticate(LoginRequest loginRequest);
    
    /**
     * Valida si la sesión del usuario es válida
     * 
     * @param token Token de acceso
     * @return true si la sesión es válida, false en caso contrario
     */
    boolean isValidSession(String token);
    
    /**
     * Indica el tipo de estrategia de autenticación
     * 
     * @return Nombre de la estrategia
     */
    String getStrategyType();
}
