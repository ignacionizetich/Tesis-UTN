package com.EDJ.ArCash.Security;

import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.RefreshToken;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.CredentialRepository;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caracterizacion del JwtAuthenticationFilter contra la cadena real.
 *
 * Pega a GET /api/accounts/user-accounts porque exige autenticacion y, con el
 * usuario recien persistido y sin cuentas, responde 200 con lista vacia.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${spring.jwt.secret}")
    private String jwtSecret;

    private User usuario;
    private Key secretKey;

    @BeforeEach
    void setUp() {
        secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));

        usuario = new User();
        usuario.setName("Ana");
        usuario.setLastName("Gomez");
        usuario.setDni("12345678");
        usuario.setEmail("ana.filtro@test.com");
        usuario.setAlias("ana.filtro");
        usuario.setEnabled(true);
        usuario.setActive(true);
        usuario.setPermissions(Permissions.USER);
        usuario = userRepository.save(usuario);

        Credentials credentials = new Credentials(usuario, "ana.filtro", passwordEncoder.encode("clave"));
        credentialRepository.save(credentials);
        usuario.setCredentials(credentials);
    }

    @Test
    @DisplayName("Sin header Authorization la cadena responde 401")
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/accounts/user-accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un access token valido con sesion activa entra")
    void tokenValidoConSesionActivaDevuelve200() throws Exception {
        persistirRefreshToken(false);
        String accessToken = jwtUtils.generateToken(String.valueOf(usuario.getId()), "USER");

        mockMvc.perform(get("/api/accounts/user-accounts")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un token vencido se rechaza con 401")
    void tokenVencidoDevuelve401() throws Exception {
        persistirRefreshToken(false);
        String vencido = tokenVencido();

        mockMvc.perform(get("/api/accounts/user-accounts")
                        .header("Authorization", "Bearer " + vencido))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token con la firma alterada se rechaza con 401")
    void tokenConFirmaAlteradaDevuelve401() throws Exception {
        persistirRefreshToken(false);
        String valido = jwtUtils.generateToken(String.valueOf(usuario.getId()), "USER");
        String alterado = alterarFirma(valido);

        mockMvc.perform(get("/api/accounts/user-accounts")
                        .header("Authorization", "Bearer " + alterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token de sesion revocada se rechaza con 401")
    void tokenDeSesionRevocadaDevuelve401() throws Exception {
        persistirRefreshToken(true);
        String accessToken = jwtUtils.generateToken(String.valueOf(usuario.getId()), "USER");

        mockMvc.perform(get("/api/accounts/user-accounts")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un refresh token mandado como Bearer se rechaza con 401")
    void refreshTokenComoBearerDevuelve401() throws Exception {
        String refreshJwt = jwtUtils.generateRefreshToken(String.valueOf(usuario.getId()), "USER");
        persistirRefreshToken(refreshJwt, false);

        mockMvc.perform(get("/api/accounts/user-accounts")
                        .header("Authorization", "Bearer " + refreshJwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/refresh sigue emitiendo access token con la cookie")
    void refreshConCookieSigueFuncionando() throws Exception {
        String refreshJwt = jwtUtils.generateRefreshToken(String.valueOf(usuario.getId()), "USER");
        persistirRefreshToken(refreshJwt, false);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    private void persistirRefreshToken(boolean revoked) {
        persistirRefreshToken("refresh-de-prueba-" + usuario.getId(), revoked);
    }

    private void persistirRefreshToken(String valor, boolean revoked) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(usuario);
        refreshToken.setRefreshToken(valor);
        refreshToken.setIssuedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(revoked);
        refreshTokenRepository.save(refreshToken);
    }

    private String tokenVencido() {
        return Jwts.builder()
                .setSubject(String.valueOf(usuario.getId()))
                .claim("userID", String.valueOf(usuario.getId()))
                .claim("role", "USER")
                .claim(JwtUtils.CLAIM_TYPE, JwtUtils.TYPE_ACCESS)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000))
                .setExpiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(secretKey)
                .compact();
    }

    private String alterarFirma(String token) {
        char ultimo = token.charAt(token.length() - 1);
        char reemplazo = ultimo == 'A' ? 'B' : 'A';
        return token.substring(0, token.length() - 1) + reemplazo;
    }
}
