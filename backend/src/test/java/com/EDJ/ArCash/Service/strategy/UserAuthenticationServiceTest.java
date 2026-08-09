package com.EDJ.ArCash.Service.strategy;

import com.EDJ.ArCash.DTO.AuthDTO.LoginRequest;
import com.EDJ.ArCash.DTO.AuthDTO.LoginResponse;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.CredentialRepository;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import com.EDJ.ArCash.factory.LoginResponseFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Caracterizacion del comportamiento ACTUAL de UserAuthenticationService
 * antes de sacar la orquestacion de tokens a AuthService.
 */
class UserAuthenticationServiceTest {

    private static final long ID_USUARIO = 5L;
    private static final long ID_CUENTA = 50L;
    private static final String USERNAME = "ana.gomez";
    private static final String PASSWORD = "secreta";
    private static final String HASH = "hash";

    private CredentialRepository credentialRepository;
    private AccountRepository accountRepository;
    private PasswordEncoder passwordEncoder;
    private TokenManagementStrategy tokenManagementStrategy;
    private RefreshTokenRepository refreshTokenRepository;
    private UserAuthenticationService service;

    @BeforeEach
    void setUp() {
        credentialRepository = mock(CredentialRepository.class);
        accountRepository = mock(AccountRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokenManagementStrategy = mock(TokenManagementStrategy.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);

        service = new UserAuthenticationService();
        ReflectionTestUtils.setField(service, "credentialRepository", credentialRepository);
        ReflectionTestUtils.setField(service, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "loginResponseFactory", new LoginResponseFactoryImpl());
        ReflectionTestUtils.setField(service, "tokenManagementStrategy", tokenManagementStrategy);
        ReflectionTestUtils.setField(service, "refreshTokenRepository", refreshTokenRepository);
    }

    @Test
    @DisplayName("Usuario inexistente devuelve error sin tocar tokens")
    void usuarioInexistente() {
        when(credentialRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        LoginResponse response = service.authenticate(pedido());

        assertFalse(response.isSuccess());
        assertEquals("Usuario no encontrado", response.getMessage());
        assertNull(response.getAccessToken());
        verify(tokenManagementStrategy, never()).generateAccessToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Password incorrecta devuelve error")
    void passwordIncorrecta() {
        stubCredenciales(usuarioActivo());
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(false);

        LoginResponse response = service.authenticate(pedido());

        assertFalse(response.isSuccess());
        assertEquals("Credenciales incorrectas", response.getMessage());
        verify(tokenManagementStrategy, never()).generateAccessToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Usuario inactivo devuelve error")
    void usuarioInactivo() {
        User user = usuarioActivo();
        user.setActive(false);
        stubCredenciales(user);
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

        LoginResponse response = service.authenticate(pedido());

        assertFalse(response.isSuccess());
        assertEquals("Usuario no habilitado", response.getMessage());
        verify(tokenManagementStrategy, never()).generateAccessToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Login exitoso reusa refresh activo y emite access token")
    void loginExitosoReusaRefresh() {
        User user = usuarioActivo();
        stubCredenciales(user);
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        when(tokenManagementStrategy.getActiveRefreshToken(user)).thenReturn("refresh-existente");
        when(tokenManagementStrategy.generateAccessToken("5", "USER")).thenReturn("access-nuevo");
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.of(cuenta(user)));

        LoginResponse response = service.authenticate(pedido());

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
        User user = usuarioActivo();
        stubCredenciales(user);
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        when(tokenManagementStrategy.getActiveRefreshToken(user)).thenReturn(null);
        when(tokenManagementStrategy.generateRefreshToken("5", "USER")).thenReturn("refresh-nuevo");
        when(tokenManagementStrategy.generateAccessToken("5", "USER")).thenReturn("access-nuevo");
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.of(cuenta(user)));

        LoginResponse response = service.authenticate(pedido());

        assertTrue(response.isSuccess());
        assertEquals("refresh-nuevo", response.getRefreshToken());
        verify(tokenManagementStrategy).saveRefreshToken(user, "refresh-nuevo");
    }

    @Test
    @DisplayName("Sin cuenta ARS: error, pero los tokens ya se generaron antes del chequeo")
    void sinCuentaIgualGeneraTokensAntesDelError() {
        // Comportamiento actual: emite/guarda tokens y recien despues busca la cuenta.
        User user = usuarioActivo();
        stubCredenciales(user);
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        when(tokenManagementStrategy.getActiveRefreshToken(user)).thenReturn(null);
        when(tokenManagementStrategy.generateRefreshToken("5", "USER")).thenReturn("refresh-nuevo");
        when(tokenManagementStrategy.generateAccessToken("5", "USER")).thenReturn("access-nuevo");
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.empty());

        LoginResponse response = service.authenticate(pedido());

        assertFalse(response.isSuccess());
        assertEquals("Cuenta no encontrada", response.getMessage());
        verify(tokenManagementStrategy).saveRefreshToken(user, "refresh-nuevo");
        verify(tokenManagementStrategy).generateAccessToken("5", "USER");
    }

    @Test
    @DisplayName("isValidSession es true si el access token apunta a un usuario con refresh activo")
    void sesionValida() {
        when(tokenManagementStrategy.extractUserId("token")).thenReturn("5");
        when(refreshTokenRepository.existsByUser_IdAndRevokedFalse(5L)).thenReturn(true);

        assertTrue(service.isValidSession("token"));
    }

    @Test
    @DisplayName("isValidSession es false si no hay refresh activo")
    void sesionInvalidaSinRefresh() {
        when(tokenManagementStrategy.extractUserId("token")).thenReturn("5");
        when(refreshTokenRepository.existsByUser_IdAndRevokedFalse(5L)).thenReturn(false);

        assertFalse(service.isValidSession("token"));
    }

    @Test
    @DisplayName("isValidSession es false si no se puede extraer el userId")
    void sesionInvalidaSinUserId() {
        when(tokenManagementStrategy.extractUserId("token")).thenReturn(null);

        assertFalse(service.isValidSession("token"));
    }

    private LoginRequest pedido() {
        return LoginRequest.builder().username(USERNAME).password(PASSWORD).build();
    }

    private void stubCredenciales(User user) {
        Credentials credentials = new Credentials(user, USERNAME, HASH);
        user.setCredentials(credentials);
        when(credentialRepository.findByUsername(USERNAME)).thenReturn(Optional.of(credentials));
    }

    private User usuarioActivo() {
        User user = new User();
        user.setId(ID_USUARIO);
        user.setActive(true);
        user.setEnabled(true);
        user.setPermissions(Permissions.USER);
        user.setAlias(USERNAME);
        return user;
    }

    private Account cuenta(User user) {
        Account account = new Account();
        account.setIdAccount(ID_CUENTA);
        account.setUser(user);
        return account;
    }
}
