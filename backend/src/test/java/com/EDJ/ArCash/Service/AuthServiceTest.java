package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.DTO.AuthDTO.LoginRequest;
import com.EDJ.ArCash.DTO.AuthDTO.LoginResponse;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Service.strategy.AuthenticationResult;
import com.EDJ.ArCash.Service.strategy.AuthenticationStrategy;
import com.EDJ.ArCash.Service.strategy.PasswordRecoveryStrategy;
import com.EDJ.ArCash.Service.strategy.TokenManagementStrategy;
import com.EDJ.ArCash.factory.LoginResponseFactory;
import com.EDJ.ArCash.factory.LoginResponseFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Caracterizacion del login orquestado por AuthService.
 * Incluye el quirk historico: sin cuenta ARS igual se emiten tokens antes del error.
 */
class AuthServiceTest {

    private static final long ID_USUARIO = 5L;
    private static final long ID_CUENTA = 50L;

    private AuthenticationStrategy authenticationStrategy;
    private TokenManagementStrategy tokenManagementStrategy;
    private AccountRepository accountRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authenticationStrategy = mock(AuthenticationStrategy.class);
        tokenManagementStrategy = mock(TokenManagementStrategy.class);
        PasswordRecoveryStrategy passwordRecoveryStrategy = mock(PasswordRecoveryStrategy.class);
        LoginResponseFactory loginResponseFactory = new LoginResponseFactoryImpl();
        accountRepository = mock(AccountRepository.class);

        authService = new AuthService();
        ReflectionTestUtils.setField(authService, "authenticationStrategy", authenticationStrategy);
        ReflectionTestUtils.setField(authService, "tokenManagementStrategy", tokenManagementStrategy);
        ReflectionTestUtils.setField(authService, "passwordRecoveryStrategy", passwordRecoveryStrategy);
        ReflectionTestUtils.setField(authService, "loginResponseFactory", loginResponseFactory);
        ReflectionTestUtils.setField(authService, "accountRepository", accountRepository);
    }

    @Test
    @DisplayName("Usuario inexistente / fallo de credenciales: error sin tocar tokens")
    void falloDeCredencialesNoEmiteTokens() {
        LoginRequest request = pedido();
        when(authenticationStrategy.authenticate(request))
                .thenReturn(AuthenticationResult.failure("Usuario no encontrado"));

        LoginResponse response = authService.login(request);

        assertFalse(response.isSuccess());
        assertEquals("Usuario no encontrado", response.getMessage());
        verify(tokenManagementStrategy, never()).generateAccessToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Login exitoso reusa refresh activo y emite access token")
    void loginExitosoReusaRefresh() {
        LoginRequest request = pedido();
        User user = usuario();
        when(authenticationStrategy.authenticate(request)).thenReturn(AuthenticationResult.success(user));
        when(tokenManagementStrategy.getActiveRefreshToken(user)).thenReturn("refresh-existente");
        when(tokenManagementStrategy.generateAccessToken("5", "USER")).thenReturn("access-nuevo");
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.of(cuenta(user)));

        LoginResponse response = authService.login(request);

        assertTrue(response.isSuccess());
        assertEquals("Inicio de sesión exitoso!", response.getMessage());
        assertEquals("access-nuevo", response.getAccessToken());
        assertEquals("refresh-existente", response.getRefreshToken());
        assertEquals(ID_CUENTA, response.getAccountId());
        assertEquals("USER", response.getRole());
        verify(tokenManagementStrategy, never()).generateRefreshToken(anyString(), anyString());
        verify(tokenManagementStrategy, never()).saveRefreshToken(any(), anyString());
    }

    @Test
    @DisplayName("Login exitoso sin refresh activo genera y guarda uno nuevo")
    void loginExitosoGeneraRefreshNuevo() {
        LoginRequest request = pedido();
        User user = usuario();
        when(authenticationStrategy.authenticate(request)).thenReturn(AuthenticationResult.success(user));
        when(tokenManagementStrategy.getActiveRefreshToken(user)).thenReturn(null);
        when(tokenManagementStrategy.generateRefreshToken("5", "USER")).thenReturn("refresh-nuevo");
        when(tokenManagementStrategy.generateAccessToken("5", "USER")).thenReturn("access-nuevo");
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.of(cuenta(user)));

        LoginResponse response = authService.login(request);

        assertTrue(response.isSuccess());
        assertEquals("refresh-nuevo", response.getRefreshToken());
        verify(tokenManagementStrategy).saveRefreshToken(user, "refresh-nuevo");
    }

    @Test
    @DisplayName("Sin cuenta ARS: error, pero los tokens ya se generaron antes del chequeo")
    void sinCuentaIgualGeneraTokensAntesDelError() {
        // Comportamiento actual: emite/guarda tokens y recien despues busca la cuenta.
        LoginRequest request = pedido();
        User user = usuario();
        when(authenticationStrategy.authenticate(request)).thenReturn(AuthenticationResult.success(user));
        when(tokenManagementStrategy.getActiveRefreshToken(user)).thenReturn(null);
        when(tokenManagementStrategy.generateRefreshToken("5", "USER")).thenReturn("refresh-nuevo");
        when(tokenManagementStrategy.generateAccessToken("5", "USER")).thenReturn("access-nuevo");
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.empty());

        LoginResponse response = authService.login(request);

        assertFalse(response.isSuccess());
        assertEquals("Cuenta no encontrada", response.getMessage());
        verify(tokenManagementStrategy).saveRefreshToken(user, "refresh-nuevo");
        verify(tokenManagementStrategy).generateAccessToken("5", "USER");
    }

    @Test
    @DisplayName("isValidSession delega en TokenManagementStrategy")
    void isValidSessionDelegaEnTokenStrategy() {
        when(tokenManagementStrategy.isValidSession("token")).thenReturn(true);

        assertTrue(authService.isValidSession("token"));
        verify(tokenManagementStrategy).isValidSession("token");
    }

    private LoginRequest pedido() {
        return LoginRequest.builder().username("ana.gomez").password("secreta").build();
    }

    private User usuario() {
        User user = new User();
        user.setId(ID_USUARIO);
        user.setActive(true);
        user.setPermissions(Permissions.USER);
        return user;
    }

    private Account cuenta(User user) {
        Account account = new Account();
        account.setIdAccount(ID_CUENTA);
        account.setUser(user);
        return account;
    }
}
