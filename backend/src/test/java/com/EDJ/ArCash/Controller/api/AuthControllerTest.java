package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.AuthDTO.LoginResponse;
import com.EDJ.ArCash.Service.AuthService;
import com.EDJ.ArCash.Service.RefreshAccessResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP de /api/auth/login para el caso sin cuenta ARS.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("Login sin cuenta ARS sigue devolviendo 401 con mensaje Cuenta no encontrada")
    void loginSinCuentaDevuelve401ConMismoMensaje() throws Exception {
        when(authService.login(any())).thenReturn(
                LoginResponse.builder()
                        .success(false)
                        .message("Cuenta no encontrada")
                        .accessToken(null)
                        .refreshToken(null)
                        .accountId(null)
                        .role(null)
                        .build()
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana.gomez\",\"password\":\"secreta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cuenta no encontrada"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("Refresh sin cookie: 400 Refresh token requerido")
    void refreshSinCookieDevuelve400() throws Exception {
        when(authService.refreshAccessToken(null)).thenReturn(RefreshAccessResult.missing());

        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Refresh token requerido"));
    }

    @Test
    @DisplayName("Refresh invalido: 401")
    void refreshInvalidoDevuelve401() throws Exception {
        when(authService.refreshAccessToken("bad")).thenReturn(RefreshAccessResult.invalid());

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refreshToken", "bad")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Refresh token inválido o expirado"));
    }

    @Test
    @DisplayName("Refresh valido: 200 con accessToken")
    void refreshValidoDevuelveAccessToken() throws Exception {
        when(authService.refreshAccessToken("ok")).thenReturn(RefreshAccessResult.ok("access-xyz"));

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refreshToken", "ok")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-xyz"));
    }
}
