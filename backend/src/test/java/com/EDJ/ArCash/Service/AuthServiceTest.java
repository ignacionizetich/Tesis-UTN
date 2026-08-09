package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.DTO.AuthDTO.LoginRequest;
import com.EDJ.ArCash.DTO.AuthDTO.LoginResponse;
import com.EDJ.ArCash.Service.strategy.AuthenticationStrategy;
import com.EDJ.ArCash.Service.strategy.PasswordRecoveryStrategy;
import com.EDJ.ArCash.Service.strategy.TokenManagementStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Caracterizacion de AuthService como fachada antes de mover la orquestacion
 * del login fuera de UserAuthenticationService.
 */
class AuthServiceTest {

    private AuthenticationStrategy authenticationStrategy;
    private TokenManagementStrategy tokenManagementStrategy;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authenticationStrategy = mock(AuthenticationStrategy.class);
        tokenManagementStrategy = mock(TokenManagementStrategy.class);
        PasswordRecoveryStrategy passwordRecoveryStrategy = mock(PasswordRecoveryStrategy.class);

        authService = new AuthService();
        ReflectionTestUtils.setField(authService, "authenticationStrategy", authenticationStrategy);
        ReflectionTestUtils.setField(authService, "tokenManagementStrategy", tokenManagementStrategy);
        ReflectionTestUtils.setField(authService, "passwordRecoveryStrategy", passwordRecoveryStrategy);
    }

    @Test
    @DisplayName("login delega en AuthenticationStrategy y devuelve su respuesta")
    void loginDelegaEnLaStrategy() {
        LoginRequest request = LoginRequest.builder().username("ana").password("x").build();
        LoginResponse esperado = LoginResponse.builder().success(true).message("ok").build();
        when(authenticationStrategy.authenticate(request)).thenReturn(esperado);

        assertSame(esperado, authService.login(request));
        verify(authenticationStrategy).authenticate(request);
    }

    @Test
    @DisplayName("isValidSession delega en AuthenticationStrategy")
    void isValidSessionDelegaEnLaStrategyDeAuth() {
        when(authenticationStrategy.isValidSession("token")).thenReturn(true);

        assertTrue(authService.isValidSession("token"));
        verify(authenticationStrategy).isValidSession("token");
    }
}
