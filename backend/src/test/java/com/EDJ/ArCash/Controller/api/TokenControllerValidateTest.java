package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Service.EmailActivationResult;
import com.EDJ.ArCash.Service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP de /api/auth/validate (mapping desde EmailActivationResult).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TokenControllerValidateTest {

    private static final String TOKEN = "token-validacion-abc";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("Token valido: 200 success=true")
    void tokenValidoDevuelve200() throws Exception {
        when(userService.activateWithToken(TOKEN)).thenReturn(EmailActivationResult.ok());

        mockMvc.perform(get("/api/auth/validate").param("token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("¡Cuenta verificada exitosamente! Ya puedes iniciar sesión."));
    }

    @Test
    @DisplayName("Token ya usado: 400")
    void tokenYaUsadoDevuelve400() throws Exception {
        when(userService.activateWithToken(TOKEN)).thenReturn(EmailActivationResult.alreadyUsed());

        mockMvc.perform(get("/api/auth/validate").param("token", TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "Este enlace de verificación ya fue utilizado. Tu cuenta ya está activada."));
    }

    @Test
    @DisplayName("Token ausente: 400")
    void tokenAusenteDevuelve400() throws Exception {
        when(userService.activateWithToken(null)).thenReturn(EmailActivationResult.missingToken());

        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token no proporcionado"));
    }
}
