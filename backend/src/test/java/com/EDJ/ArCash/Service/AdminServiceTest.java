package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.DTO.AuthDTO.UserResponse;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Caracterizacion de AdminService: listado por enabled y soft-disable.
 */
class AdminServiceTest {

    private UserRepository userRepository;
    private SessionService sessionService;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        sessionService = mock(SessionService.class);
        adminService = new AdminService();
        ReflectionTestUtils.setField(adminService, "userRepository", userRepository);
        ReflectionTestUtils.setField(adminService, "sessionService", sessionService);
    }

    @Test
    @DisplayName("getAuthUsers lista por enabled=true e incluye usuarios con active=false")
    void listadoIncluyeDeshabilitadosSiSiguenEnabled() {
        User activo = usuario(1L, true, true);
        User deshabilitado = usuario(2L, true, false);
        when(userRepository.findByEnabledTrue()).thenReturn(List.of(activo, deshabilitado));

        List<UserResponse> lista = adminService.getAuthUsers();

        assertEquals(2, lista.size());
        assertTrue(lista.get(0).isActive());
        assertFalse(lista.get(1).isActive());
        assertTrue(lista.get(1).isEnabled());
    }

    @Test
    @DisplayName("disableUser pone active=false y revoca tokens de sesion")
    void disableUserRevocaSesion() {
        User user = usuario(5L, true, true);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        adminService.disableUser(5L);

        assertFalse(user.isActive());
        verify(userRepository).save(user);
        verify(sessionService).revokeAllUserTokens(5L);
    }

    private User usuario(Long id, boolean enabled, boolean active) {
        User user = new User();
        user.setId(id);
        user.setName("Ana");
        user.setLastName("Gomez");
        user.setDni(String.valueOf(id));
        user.setEmail(id + "@test.com");
        user.setAlias("user" + id);
        user.setEnabled(enabled);
        user.setActive(active);
        user.setPermissions(Permissions.USER);
        return user;
    }
}
