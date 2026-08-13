package com.EDJ.ArCash.Service.strategy;

import com.EDJ.ArCash.DTO.AuthDTO.LoginRequest;

public interface AuthenticationStrategy {

    AuthenticationResult authenticate(LoginRequest loginRequest);

    /**
     * Indica el tipo de estrategia de autenticación.
     */
    String getStrategyType();
}
