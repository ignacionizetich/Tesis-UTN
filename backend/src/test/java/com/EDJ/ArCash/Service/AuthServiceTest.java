package com.EDJ.ArCash.Service;
import com.EDJ.ArCash.Service.result.RefreshAccessResult;
import com.EDJ.ArCash.Service.interfaces.AccountService;
import com.EDJ.ArCash.Service.interfaces.AuthService;
import com.EDJ.ArCash.Service.interfaces.RefreshTokenCleanupService;
import com.EDJ.ArCash.Service.impl.AuthServiceImpl;

import com.EDJ.ArCash.DTO.AuthDTO.LoginRequest;
import com.EDJ.ArCash.DTO.AuthDTO.LoginResponse;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.RefreshToken;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final long ID_USUARIO = 5L;
    private static final long ID_CUENTA = 50L;

    private AuthenticationStrategy authenticationStrategy;
    private TokenManagementStrategy tokenManagementStrategy;
    private AccountRepository accountRepository;
    private AccountService accountService;
    private RefreshTokenCleanupService refreshTokenCleanupService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authenticationStrategy = mock(AuthenticationStrategy.class);
        tokenManagementStrategy = mock(TokenManagementStrategy.class);
        PasswordRecoveryStrategy passwordRecoveryStrategy = mock(PasswordRecoveryStrategy.class);
        LoginResponseFactory loginResponseFactory = new LoginResponseFactoryImpl();
        accountRepository = mock(AccountRepository.class);
        accountService = mock(AccountService.class);
        refreshTokenCleanupService = mock(RefreshTokenCleanupService.class);

        authService = new AuthServiceImpl();
        ReflectionTestUtils.setField(authService, "authenticationStrategy", authenticationStrategy);
        ReflectionTestUtils.setField(authService, "tokenManagementStrategy", tokenManagementStrategy);
        ReflectionTestUtils.setField(authService, "passwordRecoveryStrategy", passwordRecoveryStrategy);
        ReflectionTestUtils.setField(authService, "loginResponseFactory", loginResponseFactory);
        ReflectionTestUtils.setField(authService, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(authService, "accountService", accountService);
        ReflectionTestUtils.setField(authService, "refreshTokenCleanupService", refreshTokenCleanupService);
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
        verify(tokenManagementStrategy, never()).saveRefreshToken(any(), anyString());
    }

    @Test
    @DisplayName("Sin ninguna cuenta: auto-crea ARS y completa el login")
    void sinCuentasCreaArsYLoginOk() {
        LoginRequest request = pedido();
        User user = usuario();
        Account ars = cuenta(user);
        when(authenticationStrategy.authenticate(request)).thenReturn(AuthenticationResult.success(user));
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.empty());
        when(accountRepository.findAllByUser_Id(ID_USUARIO)).thenReturn(List.of());
        when(accountService.ensureArsAccount(user)).thenReturn(ars);
        when(tokenManagementStrategy.getActiveRefreshToken(user)).thenReturn("refresh");
        when(tokenManagementStrategy.generateAccessToken("5", "USER")).thenReturn("access");

        LoginResponse response = authService.login(request);

        assertTrue(response.isSuccess());
        assertEquals(ID_CUENTA, response.getAccountId());
        verify(accountService).ensureArsAccount(user);
        verify(tokenManagementStrategy).generateAccessToken("5", "USER");
    }

    @Test
    @DisplayName("Tiene cuentas pero no ARS: Cuenta no encontrada y no emite tokens")
    void conCuentasNoArsNoPersisteRefreshToken() {
        LoginRequest request = pedido();
        User user = usuario();
        Account usd = cuenta(user);
        usd.setAccountType(com.EDJ.ArCash.Models.Imp.Currency.USD);
        when(authenticationStrategy.authenticate(request)).thenReturn(AuthenticationResult.success(user));
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.empty());
        when(accountRepository.findAllByUser_Id(ID_USUARIO)).thenReturn(List.of(usd));

        LoginResponse response = authService.login(request);

        assertFalse(response.isSuccess());
        assertEquals("Cuenta no encontrada", response.getMessage());
        assertNull(response.getAccessToken());
        assertNull(response.getRefreshToken());
        verify(accountService, never()).ensureArsAccount(any());
        verify(tokenManagementStrategy, never()).getActiveRefreshToken(any());
        verify(tokenManagementStrategy, never()).generateRefreshToken(anyString(), anyString());
        verify(tokenManagementStrategy, never()).saveRefreshToken(any(), anyString());
        verify(tokenManagementStrategy, never()).generateAccessToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Login con cuenta ARS: reusa refresh activo y emite access token")
    void loginExitosoReusaRefresh() {
        LoginRequest request = pedido();
        User user = usuario();
        when(authenticationStrategy.authenticate(request)).thenReturn(AuthenticationResult.success(user));
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.of(cuenta(user)));
        when(tokenManagementStrategy.getActiveRefreshToken(user)).thenReturn("refresh-existente");
        when(tokenManagementStrategy.generateAccessToken("5", "USER")).thenReturn("access-nuevo");

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
    @DisplayName("Login con cuenta ARS sin refresh activo: genera y persiste refresh, emite access")
    void loginExitosoGeneraYPersisteRefreshNuevo() {
        LoginRequest request = pedido();
        User user = usuario();
        when(authenticationStrategy.authenticate(request)).thenReturn(AuthenticationResult.success(user));
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.of(cuenta(user)));
        when(tokenManagementStrategy.getActiveRefreshToken(user)).thenReturn(null);
        when(tokenManagementStrategy.generateRefreshToken("5", "USER")).thenReturn("refresh-nuevo");
        when(tokenManagementStrategy.generateAccessToken("5", "USER")).thenReturn("access-nuevo");

        LoginResponse response = authService.login(request);

        assertTrue(response.isSuccess());
        assertEquals("refresh-nuevo", response.getRefreshToken());
        assertEquals("access-nuevo", response.getAccessToken());
        assertEquals(ID_CUENTA, response.getAccountId());
        verify(tokenManagementStrategy).saveRefreshToken(user, "refresh-nuevo");
    }

    @Test
    @DisplayName("isValidSession delega en TokenManagementStrategy")
    void isValidSessionDelegaEnTokenStrategy() {
        when(tokenManagementStrategy.isValidSession("token")).thenReturn(true);

        assertTrue(authService.isValidSession("token"));
        verify(tokenManagementStrategy).isValidSession("token");
    }

    @Test
    @DisplayName("refresh sin cookie: MISSING")
    void refreshSinCookie() {
        RefreshAccessResult r = authService.refreshAccessToken(null);
        assertEquals(RefreshAccessResult.Kind.MISSING, r.getKind());
        assertEquals("Refresh token requerido", r.getError());
        verify(refreshTokenCleanupService, never()).getRefreshTokenAndRevokedFalse(any());
    }

    @Test
    @DisplayName("refresh token inexistente o expirado: INVALID")
    void refreshInvalidoOExpirado() {
        when(refreshTokenCleanupService.getRefreshTokenAndRevokedFalse("ghost")).thenReturn(Optional.empty());
        assertEquals(RefreshAccessResult.Kind.INVALID, authService.refreshAccessToken("ghost").getKind());

        RefreshToken expired = new RefreshToken();
        expired.setUser(usuario());
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(refreshTokenCleanupService.getRefreshTokenAndRevokedFalse("exp")).thenReturn(Optional.of(expired));
        assertEquals(RefreshAccessResult.Kind.INVALID, authService.refreshAccessToken("exp").getKind());
        verify(tokenManagementStrategy, never()).generateAccessToken(anyString(), anyString());
    }

    @Test
    @DisplayName("refresh valido: emite access token")
    void refreshValidoEmiteAccess() {
        RefreshToken active = new RefreshToken();
        active.setUser(usuario());
        active.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(refreshTokenCleanupService.getRefreshTokenAndRevokedFalse("ok")).thenReturn(Optional.of(active));
        when(tokenManagementStrategy.generateAccessToken("5", "USER")).thenReturn("access-nuevo");

        RefreshAccessResult r = authService.refreshAccessToken("ok");
        assertEquals(RefreshAccessResult.Kind.OK, r.getKind());
        assertEquals("access-nuevo", r.getAccessToken());
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
