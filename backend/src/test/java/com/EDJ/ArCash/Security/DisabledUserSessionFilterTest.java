package com.EDJ.ArCash.Security;

import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.RefreshToken;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.CredentialRepository;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.Service.AdminService;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * disableUser corta la sesion de inmediato: revoca refresh y el filtro rechaza
 * active=false con "Cuenta deshabilitada".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DisabledUserSessionFilterTest {

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

    @Autowired
    private AdminService adminService;

    private User usuario;

    @BeforeEach
    void setUp() {
        usuario = new User();
        usuario.setName("Ana");
        usuario.setLastName("Gomez");
        usuario.setDni("87654321");
        usuario.setEmail("ana.disable@test.com");
        usuario.setAlias("ana.disable");
        usuario.setEnabled(true);
        usuario.setActive(true);
        usuario.setPermissions(Permissions.USER);
        usuario = userRepository.save(usuario);

        Credentials credentials = new Credentials(usuario, "ana.disable", passwordEncoder.encode("clave"));
        credentialRepository.save(credentials);
        usuario.setCredentials(credentials);
    }

    @Test
    @DisplayName("Usuario deshabilitado: se revoca el refresh y el access vigente recibe 401 Cuenta deshabilitada")
    void usuarioDeshabilitadoQuedaFueraDeInmediato() throws Exception {
        RefreshToken refresh = persistirRefreshActivo();
        String accessToken = jwtService.generateToken(String.valueOf(usuario.getId()), "USER");

        mockMvc.perform(get("/api/accounts/user-accounts")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        adminService.disableUser(usuario.getId());
        userRepository.flush();
        refreshTokenRepository.flush();

        assertFalse(userRepository.findById(usuario.getId()).orElseThrow().isActive());
        assertTrue(refreshTokenRepository.findById(refresh.getId()).orElseThrow().isRevoked());

        mockMvc.perform(get("/api/accounts/user-accounts")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Cuenta deshabilitada"));
    }

    private RefreshToken persistirRefreshActivo() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(usuario);
        refreshToken.setRefreshToken("refresh-disable-" + usuario.getId());
        refreshToken.setIssuedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);
        return refreshTokenRepository.save(refreshToken);
    }
}
