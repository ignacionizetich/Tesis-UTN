package com.EDJ.ArCash.Security;

import com.EDJ.ArCash.Models.RefreshToken;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
 * Caracterizacion de los metodos de sesion de JwtUtils antes de extraerlos
 * a SessionService.
 */
class JwtUtilsSessionTest {

    private static final long ID_USUARIO = 7L;
    private static final String SECRET =
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private JwtUtils jwtUtils;
    private User usuario;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        jwtUtils = new JwtUtils(SECRET);
        ReflectionTestUtils.setField(jwtUtils, "userRepository", userRepository);
        ReflectionTestUtils.setField(jwtUtils, "refreshTokenRepository", refreshTokenRepository);

        usuario = new User();
        usuario.setId(ID_USUARIO);
    }

    @Test
    @DisplayName("tieneSesionActiva es true si hay refresh token sin revocar")
    void sesionActivaSiHayRefreshNoRevocado() {
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
        when(refreshTokenRepository.findByUserAndRevokedFalse(usuario))
                .thenReturn(Optional.of(new RefreshToken()));

        assertTrue(jwtUtils.tieneSesionActiva(ID_USUARIO));
    }

    @Test
    @DisplayName("tieneSesionActiva es false si el usuario no existe")
    void sesionInactivaSiElUsuarioNoExiste() {
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.empty());

        assertFalse(jwtUtils.tieneSesionActiva(ID_USUARIO));
        verify(refreshTokenRepository, never()).findByUserAndRevokedFalse(any());
    }

    @Test
    @DisplayName("tieneSesionActiva es false si no hay refresh activo")
    void sesionInactivaSinRefreshActivo() {
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
        when(refreshTokenRepository.findByUserAndRevokedFalse(usuario)).thenReturn(Optional.empty());

        assertFalse(jwtUtils.tieneSesionActiva(ID_USUARIO));
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

        jwtUtils.revokeAllUserTokens(ID_USUARIO);

        assertTrue(uno.isRevoked());
        assertTrue(dos.isRevoked());
        verify(refreshTokenRepository).save(uno);
        verify(refreshTokenRepository).save(dos);
    }

    @Test
    @DisplayName("revokeAllUserTokens no hace nada si el usuario no existe")
    void revocarUsuarioInexistenteNoTocaLaBase() {
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.empty());

        jwtUtils.revokeAllUserTokens(ID_USUARIO);

        verify(refreshTokenRepository, never()).findAllByUserAndRevokedFalse(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("revokeAllUserTokens no guarda nada si no hay tokens activos")
    void revocarSinTokensActivosNoGuarda() {
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
        when(refreshTokenRepository.findAllByUserAndRevokedFalse(usuario)).thenReturn(List.of());

        jwtUtils.revokeAllUserTokens(ID_USUARIO);

        verify(refreshTokenRepository, never()).save(any());
    }
}
