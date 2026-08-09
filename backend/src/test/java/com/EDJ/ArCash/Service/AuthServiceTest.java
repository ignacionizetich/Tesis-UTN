package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.DTO.AuthDTO.LoginRequest;
import com.EDJ.ArCash.DTO.AuthDTO.LoginResponse;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.CredentialRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.Service.strategy.AuthenticationResult;
import com.EDJ.ArCash.Service.strategy.AuthenticationStrategy;
import com.EDJ.ArCash.Service.strategy.PasswordRecoveryStrategy;
import com.EDJ.ArCash.Service.strategy.TokenManagementStrategy;
import com.EDJ.ArCash.factory.LoginResponseFactory;
import com.EDJ.ArCash.factory.LoginResponseFactoryImpl;
import com.EDJ.ArCash.observer.Event;
import com.EDJ.ArCash.observer.EventPublisher;
import com.EDJ.ArCash.observer.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

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

/**
 * Login orquestado por AuthService: cuenta ARS antes de emitir/persistir tokens.
 */
class AuthServiceTest {

    private static final long ID_USUARIO = 5L;
    private static final long ID_CUENTA = 50L;

    private AuthenticationStrategy authenticationStrategy;
    private TokenManagementStrategy tokenManagementStrategy;
    private AccountRepository accountRepository;
    private UserRepository userRepository;
    private CredentialRepository credentialRepository;
    private EventPublisher eventPublisher;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authenticationStrategy = mock(AuthenticationStrategy.class);
        tokenManagementStrategy = mock(TokenManagementStrategy.class);
        PasswordRecoveryStrategy passwordRecoveryStrategy = mock(PasswordRecoveryStrategy.class);
        LoginResponseFactory loginResponseFactory = new LoginResponseFactoryImpl();
        accountRepository = mock(AccountRepository.class);
        userRepository = mock(UserRepository.class);
        credentialRepository = mock(CredentialRepository.class);
        eventPublisher = mock(EventPublisher.class);

        authService = new AuthService();
        ReflectionTestUtils.setField(authService, "authenticationStrategy", authenticationStrategy);
        ReflectionTestUtils.setField(authService, "tokenManagementStrategy", tokenManagementStrategy);
        ReflectionTestUtils.setField(authService, "passwordRecoveryStrategy", passwordRecoveryStrategy);
        ReflectionTestUtils.setField(authService, "loginResponseFactory", loginResponseFactory);
        ReflectionTestUtils.setField(authService, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(authService, "userRepository", userRepository);
        ReflectionTestUtils.setField(authService, "credentialRepository", credentialRepository);
        ReflectionTestUtils.setField(authService, "eventPublisher", eventPublisher);
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
    @DisplayName("Sin cuenta ARS: mismo mensaje de error y no se persiste ningun refresh token")
    void sinCuentaNoPersisteRefreshToken() {
        LoginRequest request = pedido();
        User user = usuario();
        when(authenticationStrategy.authenticate(request)).thenReturn(AuthenticationResult.success(user));
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.empty());

        LoginResponse response = authService.login(request);

        assertFalse(response.isSuccess());
        assertEquals("Cuenta no encontrada", response.getMessage());
        assertNull(response.getAccessToken());
        assertNull(response.getRefreshToken());
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
    @DisplayName("cambiarAliasYUsername rechaza formato invalido")
    void cambiarAliasFormatoInvalido() {
        assertFalse(authService.cambiarAliasYUsername(ID_USUARIO, "ab"));
        assertFalse(authService.cambiarAliasYUsername(ID_USUARIO, "12345"));
        assertFalse(authService.cambiarAliasYUsername(ID_USUARIO, null));
        verify(accountRepository, never()).findByUser_Id(any());
    }

    @Test
    @DisplayName("cambiarAliasYUsername falla si no hay cuenta ARS")
    void cambiarAliasSinCuenta() {
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.empty());

        assertFalse(authService.cambiarAliasYUsername(ID_USUARIO, "nuevoalias"));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("cambiarAliasYUsername falla si el username ya esta tomado")
    void cambiarAliasUsernameTomado() {
        User user = usuarioConCredenciales("viejoalias");
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.of(cuenta(user)));
        when(credentialRepository.findByUsername("nuevoalias")).thenReturn(Optional.of(new Credentials()));

        assertFalse(authService.cambiarAliasYUsername(ID_USUARIO, "nuevoalias"));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("cambiarAliasYUsername exitoso actualiza alias/username y publica ALIAS_CHANGED")
    void cambiarAliasExitoso() {
        User user = usuarioConCredenciales("viejoalias");
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.of(cuenta(user)));
        when(credentialRepository.findByUsername("nuevoalias")).thenReturn(Optional.empty());

        assertTrue(authService.cambiarAliasYUsername(ID_USUARIO, "nuevoalias"));

        assertEquals("nuevoalias", user.getAlias());
        assertEquals("nuevoalias", user.getCredentials().getUsername());
        verify(userRepository).saveAndFlush(user);
        verify(credentialRepository).save(user.getCredentials());

        ArgumentCaptor<Event> eventoCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisher).publish(eventoCaptor.capture());
        Event evento = eventoCaptor.getValue();
        assertEquals(EventType.ALIAS_CHANGED, evento.getEventType());
        assertEquals("viejoalias", evento.getData("oldAlias"));
        assertEquals("nuevoalias", evento.getData("newAlias"));
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

    private User usuarioConCredenciales(String alias) {
        User user = usuario();
        user.setAlias(alias);
        Credentials credentials = new Credentials(user, alias, "hash");
        user.setCredentials(credentials);
        return user;
    }

    private Account cuenta(User user) {
        Account account = new Account();
        account.setIdAccount(ID_CUENTA);
        account.setUser(user);
        return account;
    }
}

