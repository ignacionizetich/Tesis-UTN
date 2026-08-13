package com.EDJ.ArCash.Service;
import com.EDJ.ArCash.Service.result.EmailActivationResult;
import com.EDJ.ArCash.Service.result.RegistrationConflictCode;
import com.EDJ.ArCash.Service.result.RegistrationConflictException;
import com.EDJ.ArCash.Service.interfaces.AccountService;
import com.EDJ.ArCash.Service.interfaces.EmailService;
import com.EDJ.ArCash.Service.interfaces.UserService;
import com.EDJ.ArCash.Service.interfaces.ValidationTokenService;
import com.EDJ.ArCash.Service.impl.UserServiceImpl;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.CredentialRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.observer.Event;
import com.EDJ.ArCash.observer.EventPublisher;
import com.EDJ.ArCash.observer.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private static final String EMAIL = "ana@test.com";
    private static final String ALIAS = "ana.gomez";
    private static final String DNI = "30111222";
    private static final String PASSWORD = "secreta";
    private static final String HASH = "hash";
    private static final long ID_USUARIO = 5L;
    private static final long ID_CUENTA = 50L;

    private UserRepository userRepository;
    private AccountRepository accountRepository;
    private CredentialRepository credentialRepository;
    private AccountService accountService;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private ValidationTokenService validationTokenService;
    private EventPublisher eventPublisher;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        accountRepository = mock(AccountRepository.class);
        credentialRepository = mock(CredentialRepository.class);
        accountService = mock(AccountService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailService = mock(EmailService.class);
        validationTokenService = mock(ValidationTokenService.class);
        eventPublisher = mock(EventPublisher.class);

        userService = new UserServiceImpl(
                passwordEncoder,
                userRepository,
                accountService,
                accountRepository,
                credentialRepository,
                emailService,
                validationTokenService,
                eventPublisher
        );

        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.findByAlias(any())).thenReturn(Optional.empty());
        when(userRepository.findByDni(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Email ya existente lanza EMAIL_ALREADY_EXISTS y no guarda")
    void conflictoSoloEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new User()));

        RegistrationConflictException ex = assertThrows(RegistrationConflictException.class,
                () -> userService.insertarUsuario(usuarioNuevo(), PASSWORD));

        assertEquals(List.of(RegistrationConflictCode.EMAIL_ALREADY_EXISTS), ex.getCodes());
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Alias ya existente lanza ALIAS_ALREADY_EXISTS y no guarda")
    void conflictoSoloAlias() {
        when(userRepository.findByAlias(ALIAS)).thenReturn(Optional.of(new User()));

        RegistrationConflictException ex = assertThrows(RegistrationConflictException.class,
                () -> userService.insertarUsuario(usuarioNuevo(), PASSWORD));

        assertEquals(List.of(RegistrationConflictCode.ALIAS_ALREADY_EXISTS), ex.getCodes());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("DNI ya existente lanza DNI_ALREADY_EXISTS y no guarda")
    void conflictoSoloDni() {
        when(userRepository.findByDni(DNI)).thenReturn(Optional.of(new User()));

        RegistrationConflictException ex = assertThrows(RegistrationConflictException.class,
                () -> userService.insertarUsuario(usuarioNuevo(), PASSWORD));

        assertEquals(List.of(RegistrationConflictCode.DNI_ALREADY_EXISTS), ex.getCodes());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Varios conflictos se reportan en orden email, alias, dni")
    void conflictosCombinados() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new User()));
        when(userRepository.findByAlias(ALIAS)).thenReturn(Optional.of(new User()));
        when(userRepository.findByDni(DNI)).thenReturn(Optional.of(new User()));

        RegistrationConflictException ex = assertThrows(RegistrationConflictException.class,
                () -> userService.insertarUsuario(usuarioNuevo(), PASSWORD));

        assertEquals(List.of(
                RegistrationConflictCode.EMAIL_ALREADY_EXISTS,
                RegistrationConflictCode.ALIAS_ALREADY_EXISTS,
                RegistrationConflictCode.DNI_ALREADY_EXISTS
        ), ex.getCodes());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registro exitoso: capitaliza, desactiva, crea credenciales/token y publica evento")
    void registroExitoso() {
        User user = usuarioNuevo();

        userService.insertarUsuario(user, PASSWORD);

        assertEquals("Ana", user.getName());
        assertEquals("Gomez", user.getLastName());
        assertFalse(user.isEnabled());
        assertFalse(user.isActive());
        assertEquals(Permissions.USER, user.getPermissions());
        assertNotNull(user.getCredentials());
        assertEquals(ALIAS, user.getCredentials().getUsername());
        assertEquals(HASH, user.getCredentials().getPass());
        assertNotNull(user.getValidationToken());

        verify(userRepository).save(user);

        ArgumentCaptor<Event> eventoCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisher).publish(eventoCaptor.capture());
        Event evento = eventoCaptor.getValue();
        assertEquals(EventType.USER_REGISTERED, evento.getEventType());
        assertEquals(user, evento.getData("user"));
        assertEquals(user.getValidationToken().getToken(), evento.getData("token"));
        assertTrue(user.getValidationToken().getToken() != null
                && !user.getValidationToken().getToken().isBlank());
    }

    @Test
    @DisplayName("cambiarAliasYUsername rechaza formato invalido")
    void cambiarAliasFormatoInvalido() {
        assertFalse(userService.cambiarAliasYUsername(ID_USUARIO, "ab"));
        assertFalse(userService.cambiarAliasYUsername(ID_USUARIO, "12345"));
        assertFalse(userService.cambiarAliasYUsername(ID_USUARIO, null));
        verify(accountRepository, never()).findByUser_Id(any());
    }

    @Test
    @DisplayName("cambiarAliasYUsername falla si no hay cuenta ARS")
    void cambiarAliasSinCuenta() {
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.empty());

        assertFalse(userService.cambiarAliasYUsername(ID_USUARIO, "nuevoalias"));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("cambiarAliasYUsername falla si el username ya esta tomado")
    void cambiarAliasUsernameTomado() {
        User user = usuarioConCredenciales("viejoalias");
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.of(cuenta(user)));
        when(credentialRepository.findByUsername("nuevoalias")).thenReturn(Optional.of(new Credentials()));

        assertFalse(userService.cambiarAliasYUsername(ID_USUARIO, "nuevoalias"));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("cambiarAliasYUsername exitoso actualiza alias/username y publica ALIAS_CHANGED")
    void cambiarAliasExitoso() {
        User user = usuarioConCredenciales("viejoalias");
        when(accountRepository.findByUser_Id(ID_USUARIO)).thenReturn(Optional.of(cuenta(user)));
        when(credentialRepository.findByUsername("nuevoalias")).thenReturn(Optional.empty());

        assertTrue(userService.cambiarAliasYUsername(ID_USUARIO, "nuevoalias"));

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

    @Test
    @DisplayName("activateWithToken: token vacio → MISSING_TOKEN")
    void activateWithTokenVacio() {
        EmailActivationResult r = userService.activateWithToken("  ");
        assertEquals(EmailActivationResult.Kind.MISSING_TOKEN, r.getKind());
        verify(validationTokenService, never()).buscarToken(any());
    }

    @Test
    @DisplayName("activateWithToken: token inexistente → INVALID")
    void activateWithTokenInexistente() {
        when(validationTokenService.buscarToken("ghost")).thenReturn(Optional.empty());

        EmailActivationResult r = userService.activateWithToken("ghost");
        assertEquals(EmailActivationResult.Kind.INVALID, r.getKind());
        verify(accountService, never()).createAccount(any());
    }

    @Test
    @DisplayName("activateWithToken: token ya usado → ALREADY_USED")
    void activateWithTokenYaUsado() {
        User user = usuarioConCredenciales(ALIAS);
        com.EDJ.ArCash.Models.ValidationToken token = new com.EDJ.ArCash.Models.ValidationToken();
        token.setUser(user);
        token.setUsed(true);
        token.setExpirationDate(java.time.LocalDateTime.now().plusHours(1));
        when(validationTokenService.buscarToken("used")).thenReturn(Optional.of(token));

        EmailActivationResult r = userService.activateWithToken("used");
        assertEquals(EmailActivationResult.Kind.ALREADY_USED, r.getKind());
        verify(accountService, never()).createAccount(any());
    }

    @Test
    @DisplayName("activateWithToken: token expirado → EXPIRED")
    void activateWithTokenExpirado() {
        User user = usuarioConCredenciales(ALIAS);
        com.EDJ.ArCash.Models.ValidationToken token = new com.EDJ.ArCash.Models.ValidationToken();
        token.setUser(user);
        token.setUsed(false);
        token.setExpirationDate(java.time.LocalDateTime.now().minusMinutes(1));
        when(validationTokenService.buscarToken("exp")).thenReturn(Optional.of(token));

        EmailActivationResult r = userService.activateWithToken("exp");
        assertEquals(EmailActivationResult.Kind.EXPIRED, r.getKind());
        verify(accountService, never()).createAccount(any());
    }

    @Test
    @DisplayName("activateWithToken: valido activa usuario y marca token usado")
    void activateWithTokenOk() {
        User user = usuarioConCredenciales(ALIAS);
        user.setEnabled(false);
        user.setActive(false);
        com.EDJ.ArCash.Models.ValidationToken token = new com.EDJ.ArCash.Models.ValidationToken();
        token.setUser(user);
        token.setUsed(false);
        token.setExpirationDate(java.time.LocalDateTime.now().plusHours(1));
        when(validationTokenService.buscarToken("ok")).thenReturn(Optional.of(token));

        EmailActivationResult r = userService.activateWithToken("ok");

        assertEquals(EmailActivationResult.Kind.OK, r.getKind());
        assertTrue(user.isEnabled());
        assertTrue(user.isActive());
        verify(accountService).createAccount(user);
        verify(validationTokenService).usedToken(user);
    }

    private User usuarioNuevo() {
        return new User("ana", "gomez", DNI, EMAIL, ALIAS);
    }

    private User usuarioConCredenciales(String alias) {
        User user = new User();
        user.setId(ID_USUARIO);
        user.setActive(true);
        user.setPermissions(Permissions.USER);
        user.setAlias(alias);
        user.setCredentials(new Credentials(user, alias, "hash"));
        return user;
    }

    private Account cuenta(User user) {
        Account account = new Account();
        account.setIdAccount(ID_CUENTA);
        account.setUser(user);
        return account;
    }
}
