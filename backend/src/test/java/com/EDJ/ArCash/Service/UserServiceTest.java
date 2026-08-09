package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
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

/**
 * Caracterizacion del registro en UserService (conflictos y camino feliz).
 */
class UserServiceTest {

    private static final String EMAIL = "ana@test.com";
    private static final String ALIAS = "ana.gomez";
    private static final String DNI = "30111222";
    private static final String PASSWORD = "secreta";
    private static final String HASH = "hash";

    private UserRepository userRepository;
    private AccountService accountService;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private ValidationTokenService validationTokenService;
    private EventPublisher eventPublisher;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        accountService = mock(AccountService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailService = mock(EmailService.class);
        validationTokenService = mock(ValidationTokenService.class);
        eventPublisher = mock(EventPublisher.class);
        CredentialsService credentialsService = mock(CredentialsService.class);

        userService = new UserService(
                passwordEncoder,
                userRepository,
                accountService,
                credentialsService,
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

    private User usuarioNuevo() {
        return new User("ana", "gomez", DNI, EMAIL, ALIAS);
    }
}
