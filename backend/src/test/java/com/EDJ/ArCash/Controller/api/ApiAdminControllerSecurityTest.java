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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiAdminControllerSecurityTest {

    private static final String CREATE_ADMIN_BODY = """
            {
              "name":"Nuevo",
              "lastName":"Admin",
              "dni":"55667788",
              "email":"nuevo.admin.sec@test.com",
              "username":"nuevoadminsec",
              "password":"Secret123!"
            }
            """;

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
    private User objetivo;

    @BeforeEach
    void setUp() {
        usuario = persistirUsuario("user.admin.sec", "user.admin.sec@test.com", "11111111", Permissions.USER);
        admin = persistirUsuario("admin.sec", "admin.sec@test.com", "22222222", Permissions.ADMIN);
        objetivo = persistirUsuario("target.sec", "target.sec@test.com", "33333333", Permissions.USER);
    }

    // --- GET /users ---

    @Test
    @DisplayName("GET /users sin token: 401")
    void listarSinToken401() throws Exception {
        expectUnauthorized(get("/api/admin/users"));
    }

    @Test
    @DisplayName("GET /users con USER: 403")
    void listarConUser403() throws Exception {
        expectForbidden(get("/api/admin/users"), tokenUser());
    }

    @Test
    @DisplayName("GET /users con ADMIN: 200")
    void listarConAdmin200() throws Exception {
        expectOk(get("/api/admin/users"), tokenAdmin());
    }

    // --- PUT /users/{id}/disable ---

    @Test
    @DisplayName("PUT disable sin token: 401")
    void disableSinToken401() throws Exception {
        expectUnauthorized(put("/api/admin/users/{id}/disable", objetivo.getId()));
    }

    @Test
    @DisplayName("PUT disable con USER: 403")
    void disableConUser403() throws Exception {
        expectForbidden(put("/api/admin/users/{id}/disable", objetivo.getId()), tokenUser());
    }

    @Test
    @DisplayName("PUT disable con ADMIN: 200")
    void disableConAdmin200() throws Exception {
        expectOk(put("/api/admin/users/{id}/disable", objetivo.getId()), tokenAdmin());
    }

    // --- PUT /users/{id}/enable ---

    @Test
    @DisplayName("PUT enable sin token: 401")
    void enableSinToken401() throws Exception {
        expectUnauthorized(put("/api/admin/users/{id}/enable", objetivo.getId()));
    }

    @Test
    @DisplayName("PUT enable con USER: 403")
    void enableConUser403() throws Exception {
        expectForbidden(put("/api/admin/users/{id}/enable", objetivo.getId()), tokenUser());
    }

    @Test
    @DisplayName("PUT enable con ADMIN: 200")
    void enableConAdmin200() throws Exception {
        expectOk(put("/api/admin/users/{id}/enable", objetivo.getId()), tokenAdmin());
    }

    // --- POST /users/create-admin ---

    @Test
    @DisplayName("POST create-admin sin token: 401")
    void createAdminSinToken401() throws Exception {
        expectUnauthorized(post("/api/admin/users/create-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_ADMIN_BODY));
    }

    @Test
    @DisplayName("POST create-admin con USER: 403")
    void createAdminConUser403() throws Exception {
        expectForbidden(post("/api/admin/users/create-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_ADMIN_BODY), tokenUser());
    }

    @Test
    @DisplayName("POST create-admin con ADMIN: 200")
    void createAdminConAdmin200() throws Exception {
        expectOk(post("/api/admin/users/create-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_ADMIN_BODY), tokenAdmin());
    }

    // --- GET /check-access ---

    @Test
    @DisplayName("GET check-access sin token: 401")
    void checkAccessSinToken401() throws Exception {
        expectUnauthorized(get("/api/admin/check-access"));
    }

    @Test
    @DisplayName("GET check-access con USER: 403")
    void checkAccessConUser403() throws Exception {
        expectForbidden(get("/api/admin/check-access"), tokenUser());
    }

    @Test
    @DisplayName("GET check-access con ADMIN: 200")
    void checkAccessConAdmin200() throws Exception {
        expectOk(get("/api/admin/check-access"), tokenAdmin());
    }

    private void expectUnauthorized(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    private void expectForbidden(MockHttpServletRequestBuilder request, String token) throws Exception {
        mockMvc.perform(request.header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private void expectOk(MockHttpServletRequestBuilder request, String token) throws Exception {
        ResultActions actions = mockMvc.perform(request.header("Authorization", "Bearer " + token));
        actions.andExpect(status().isOk());
    }

    private String tokenUser() {
        return emitirAccessConSesion(usuario, "USER");
    }

    private String tokenAdmin() {
        return emitirAccessConSesion(admin, "ADMIN");
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
        user.setCredentials(credentials);
        credentialRepository.save(credentials);
        return user;
    }

    private String emitirAccessConSesion(User user, String role) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setRefreshToken("refresh-" + user.getId() + "-" + role + "-" + System.nanoTime());
        refreshToken.setIssuedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return jwtService.generateToken(String.valueOf(user.getId()), role);
    }
}
