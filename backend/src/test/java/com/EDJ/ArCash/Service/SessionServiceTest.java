package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.RefreshToken;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Caracterizacion de SessionService (antes metodos de sesion en JwtUtils).
 */
class SessionServiceTest {

    private static final long ID_USUARIO = 7L;

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private SessionService sessionService;
    private User usuario;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        sessionService = new SessionService(userRepository, refreshTokenRepository);

        usuario = new User();
        usuario.setId(ID_USUARIO);
    }

    @Test
    @DisplayName("tieneSesionActiva es true si hay refresh token sin revocar")
    void sesionActivaSiHayRefreshNoRevocado() {
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
        when(refreshTokenRepository.findByUserAndRevokedFalse(usuario))
                .thenReturn(Optional.of(new RefreshToken()));

        assertTrue(sessionService.tieneSesionActiva(ID_USUARIO));
    }

    @Test
    @DisplayName("tieneSesionActiva es false si el usuario no existe")
    void sesionInactivaSiElUsuarioNoExiste() {
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.empty());

        assertFalse(sessionService.tieneSesionActiva(ID_USUARIO));
        verify(refreshTokenRepository, never()).findByUserAndRevokedFalse(any());
    }

    @Test
    @DisplayName("tieneSesionActiva es false si no hay refresh activo")
    void sesionInactivaSinRefreshActivo() {
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
        when(refreshTokenRepository.findByUserAndRevokedFalse(usuario)).thenReturn(Optional.empty());

        assertFalse(sessionService.tieneSesionActiva(ID_USUARIO));
    }

    @Test
    @DisplayName("revokeAllUserTokens marca como revocados todos los refresh activos")
    void revocaTodosLosRefreshActivos() {
        RefreshToken uno = new RefreshToken();
        uno.setRevoked(false);
        RefreshToken dos = new RefreshToken();
        dos.setRevoked(false);
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
        when(refreshTokenRepository.findAllByUserAndRevokedFalse(usuario)).thenReturn(List.of(uno, dos));

        sessionService.revokeAllUserTokens(ID_USUARIO);

        assertTrue(uno.isRevoked());
        assertTrue(dos.isRevoked());
        verify(refreshTokenRepository).save(uno);
        verify(refreshTokenRepository).save(dos);
    }

    @Test
    @DisplayName("revokeAllUserTokens no hace nada si el usuario no existe")
    void revocarUsuarioInexistenteNoTocaLaBase() {
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.empty());

        sessionService.revokeAllUserTokens(ID_USUARIO);

        verify(refreshTokenRepository, never()).findAllByUserAndRevokedFalse(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("revokeAllUserTokens no guarda nada si no hay tokens activos")
    void revocarSinTokensActivosNoGuarda() {
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
        when(refreshTokenRepository.findAllByUserAndRevokedFalse(usuario)).thenReturn(List.of());

        sessionService.revokeAllUserTokens(ID_USUARIO);

        verify(refreshTokenRepository, never()).save(any());
    }
}
