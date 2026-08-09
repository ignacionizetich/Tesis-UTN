package com.EDJ.ArCash.Service.strategy;

import com.EDJ.ArCash.DTO.AuthDTO.LoginRequest;

/**
 * Strategy para validar credenciales de un usuario.
 * No emite ni gestiona tokens: eso lo orquesta AuthService con TokenManagementStrategy.
 */
public interface AuthenticationStrategy {

    /**
     * Valida las credenciales del pedido de login.
     *
     * @param loginRequest usuario y password
     * @return exito con usuario y cuenta, o fallo con mensaje
     */
    AuthenticationResult authenticate(LoginRequest loginRequest);

    /**
     * Indica el tipo de estrategia de autenticación.
     */
    String getStrategyType();
}
