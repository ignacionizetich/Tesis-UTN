package com.EDJ.ArCash.Service.strategy;

import com.EDJ.ArCash.DTO.AuthDTO.LoginRequest;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.CredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserAuthenticationServiceTest {

    private static final long ID_USUARIO = 5L;
    private static final String USERNAME = "ana.gomez";
    private static final String PASSWORD = "secreta";
    private static final String HASH = "hash";

    private CredentialRepository credentialRepository;
    private PasswordEncoder passwordEncoder;
    private UserAuthenticationService service;

    @BeforeEach
    void setUp() {
        credentialRepository = mock(CredentialRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserAuthenticationService(credentialRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Usuario inexistente devuelve error sin tocar tokens")
    void usuarioInexistente() {
        when(credentialRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        AuthenticationResult resultado = service.authenticate(pedido());

        assertFalse(resultado.isSuccess());
        assertEquals("Usuario no encontrado", resultado.getErrorMessage());
        assertNull(resultado.getUser());
    }

    @Test
    @DisplayName("Password incorrecta devuelve error")
    void passwordIncorrecta() {
        stubCredenciales(usuarioActivo());
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(false);

        AuthenticationResult resultado = service.authenticate(pedido());

        assertFalse(resultado.isSuccess());
        assertEquals("Credenciales incorrectas", resultado.getErrorMessage());
    }

    @Test
    @DisplayName("Usuario inactivo devuelve error")
    void usuarioInactivo() {
        User user = usuarioActivo();
        user.setActive(false);
        stubCredenciales(user);
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

        AuthenticationResult resultado = service.authenticate(pedido());

        assertFalse(resultado.isSuccess());
        assertEquals("Usuario no habilitado", resultado.getErrorMessage());
    }

    @Test
    @DisplayName("Credenciales validas: exito con el usuario (tokens los emite AuthService)")
    void credencialesValidas() {
        User user = usuarioActivo();
        stubCredenciales(user);
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

        AuthenticationResult resultado = service.authenticate(pedido());

        assertTrue(resultado.isSuccess());
        assertSame(user, resultado.getUser());
        assertNull(resultado.getErrorMessage());
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
}
