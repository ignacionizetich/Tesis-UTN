package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.RefreshToken;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.CredentialRepository;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.Security.JwtService;
import com.EDJ.ArCash.Service.result.AdminCreateResult;
import com.EDJ.ArCash.Service.interfaces.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiAdminControllerCreateAdminTest {

    private static final String BODY = """
            {
              "name":"Ana",
              "lastName":"Gomez",
              "dni":"30111222",
              "email":"nuevo.admin@test.com",
              "username":"nuevoadmin",
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

    @MockitoBean
    private AdminService adminService;

    private String adminAccessToken;

    @BeforeEach
    void setUp() {
        User admin = new User();
        admin.setName("Root");
        admin.setLastName("Admin");
        admin.setDni("99999999");
        admin.setEmail("root.create@test.com");
        admin.setAlias("root.create");
        admin.setEnabled(true);
        admin.setActive(true);
        admin.setPermissions(Permissions.ROOT);
        admin = userRepository.save(admin);

        Credentials credentials = new Credentials(admin, "root.create", passwordEncoder.encode("clave"));
        admin.setCredentials(credentials);
        credentialRepository.save(credentials);
        userRepository.flush();
        credentialRepository.flush();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(admin);
        refreshToken.setRefreshToken("refresh-create-admin-" + admin.getId());
        refreshToken.setIssuedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
        refreshTokenRepository.flush();

        // El alta de administradores es exclusiva de ROOT (@PreAuthorize en el controller).
        adminAccessToken = jwtService.generateToken(String.valueOf(admin.getId()), "ROOT");
    }

    @Test
    @DisplayName("409 por conflicto: el campo culpable viaja en fieldErrors y SIN clave detalle")
    void conflicto409SinDetalle() throws Exception {
        when(adminService.createAdmin(any())).thenReturn(
                AdminCreateResult.conflict("username", "nombre de usuario no está disponible"));

        mockMvc.perform(post("/api/admin/users/create-admin")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("nombre de usuario no está disponible"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("username"))
                .andExpect(jsonPath("$.detalle").doesNotExist());
    }

    @Test
    @DisplayName("409 generico de carrera: mensaje fijo, sin fieldErrors ni clave detalle")
    void conflictoGenerico409SinDetalle() throws Exception {
        when(adminService.createAdmin(any())).thenReturn(AdminCreateResult.conflictGeneric());

        mockMvc.perform(post("/api/admin/users/create-admin")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Error de duplicación en la base de datos"))
                // Sin campo atribuible, fieldErrors se omite en lugar de mandar una lista vacia.
                .andExpect(jsonPath("$.fieldErrors").doesNotExist())
                .andExpect(jsonPath("$.detalle").doesNotExist());
    }

    @Test
    @DisplayName("500: mensaje generico y SIN clave detalle (aunque la causa tenga texto de DB)")
    void error500SinDetalle() throws Exception {
        when(adminService.createAdmin(any())).thenReturn(AdminCreateResult.error());

        mockMvc.perform(post("/api/admin/users/create-admin")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("Error interno del servidor"))
                .andExpect(jsonPath("$.detalle").doesNotExist())
                // El nombre de la constraint de la DB no puede llegar al cliente.
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("UK22orgon"))));
    }

    @Test
    @DisplayName("Alta exitosa: 200 con el mensaje en JSON, no como texto plano")
    void altaExitosa() throws Exception {
        when(adminService.createAdmin(any())).thenReturn(AdminCreateResult.success());

        mockMvc.perform(post("/api/admin/users/create-admin")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Usuario administrador creado correctamente"));
    }
}
