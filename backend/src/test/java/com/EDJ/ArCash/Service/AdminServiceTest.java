package com.EDJ.ArCash.Service;
import com.EDJ.ArCash.Service.result.AdminCreateResult;
import com.EDJ.ArCash.Service.interfaces.AccountService;
import com.EDJ.ArCash.Service.interfaces.AdminService;
import com.EDJ.ArCash.Service.interfaces.SessionService;
import com.EDJ.ArCash.Service.impl.AdminServiceImpl;

import com.EDJ.ArCash.DTO.AuthDTO.AdminRequest;
import com.EDJ.ArCash.DTO.AuthDTO.UserResponse;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AdminService: listado, soft-disable y createAdmin.
 */
class AdminServiceTest {

    private UserRepository userRepository;
    private AccountService accountService;
    private SessionService sessionService;
    private PasswordEncoder passwordEncoder;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        accountService = mock(AccountService.class);
        sessionService = mock(SessionService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        adminService = new AdminServiceImpl();
        ReflectionTestUtils.setField(adminService, "userRepository", userRepository);
        ReflectionTestUtils.setField(adminService, "accountService", accountService);
        ReflectionTestUtils.setField(adminService, "sessionService", sessionService);
        ReflectionTestUtils.setField(adminService, "passwordEncoder", passwordEncoder);

        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.existsByAlias(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByDni(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
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

    @Test
    @DisplayName("createAdmin con username tomado: conflicto sin persistir")
    void createAdminUsernameTomado() {
        when(userRepository.existsByAlias("nuevoadmin")).thenReturn(true);

        AdminCreateResult resultado = adminService.createAdmin(pedido());

        assertEquals(AdminCreateResult.Kind.CONFLICT, resultado.getKind());
        assertEquals("username", resultado.getCampo());
        assertEquals("nombre de usuario no está disponible", resultado.getMensaje());
        verify(userRepository, never()).save(any());
        verify(accountService, never()).createAccount(any());
    }

    @Test
    @DisplayName("createAdmin exitoso capitaliza, marca ADMIN activo y crea cuenta")
    void createAdminExitoso() {
        AdminCreateResult resultado = adminService.createAdmin(pedido());

        assertTrue(resultado.isSuccess());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User guardado = userCaptor.getValue();
        assertEquals("Ana", guardado.getName());
        assertEquals("Gomez", guardado.getLastName());
        assertEquals(Permissions.ADMIN, guardado.getPermissions());
        assertTrue(guardado.isEnabled());
        assertTrue(guardado.isActive());
        assertEquals("nuevoadmin", guardado.getAlias());
        assertEquals("hash", guardado.getCredentials().getPass());
        verify(accountService).createAccount(guardado);
    }

    @Test
    @DisplayName("createAdmin ante carrera DIV sin exists: conflicto generico sin campo")
    void createAdminCarreraGenerica() {
        doThrow(new DataIntegrityViolationException("constraint secret"))
                .when(accountService).createAccount(any());

        AdminCreateResult resultado = adminService.createAdmin(pedido());

        assertEquals(AdminCreateResult.Kind.CONFLICT, resultado.getKind());
        assertEquals("Error de duplicación en la base de datos", resultado.getMensaje());
        assertNull(resultado.getCampo());
    }

    private AdminRequest pedido() {
        return AdminRequest.builder()
                .name("ana")
                .lastName("gomez")
                .dni("30111222")
                .email("nuevo.admin@test.com")
                .username("nuevoadmin")
                .password("Secret123!")
                .build();
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
