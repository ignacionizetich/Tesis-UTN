package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.RefreshToken;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.CredentialRepository;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.Security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caracterizacion de la proteccion de /api/admin/** (SecurityConfig + sesion).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User usuario;
    private User admin;

    @BeforeEach
    void setUp() {
        usuario = persistirUsuario("user.admin.sec", "user.admin.sec@test.com", "11111111", Permissions.USER);
        admin = persistirUsuario("admin.sec", "admin.sec@test.com", "22222222", Permissions.ADMIN);
    }

    @Test
    @DisplayName("GET /api/admin/users sin token: 401")
    void listarSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/admin/check-access sin token: 401")
    void checkAccessSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/admin/check-access"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Usuario ROLE_USER no entra a /api/admin/users: 403")
    void usuarioComunRecibe403() throws Exception {
        String token = emitirAccessConSesion(usuario, "USER");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin con sesion activa entra a /api/admin/users: 200")
    void adminEntraAListado() throws Exception {
        String token = emitirAccessConSesion(admin, "ADMIN");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin con sesion activa entra a /api/admin/check-access: 200")
    void adminEntraACheckAccess() throws Exception {
        String token = emitirAccessConSesion(admin, "ADMIN");

        mockMvc.perform(get("/api/admin/check-access")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private User persistirUsuario(String alias, String email, String dni, Permissions permissions) {
        User user = new User();
        user.setName("Test");
        user.setLastName("User");
        user.setDni(dni);
        user.setEmail(email);
        user.setAlias(alias);
        user.setEnabled(true);
        user.setActive(true);
        user.setPermissions(permissions);
        user = userRepository.save(user);

        Credentials credentials = new Credentials(user, alias, passwordEncoder.encode("clave"));
        credentialRepository.save(credentials);
        user.setCredentials(credentials);
        return user;
    }

    private String emitirAccessConSesion(User user, String role) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setRefreshToken("refresh-" + user.getId() + "-" + role);
        refreshToken.setIssuedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return jwtService.generateToken(String.valueOf(user.getId()), role);
    }
}
