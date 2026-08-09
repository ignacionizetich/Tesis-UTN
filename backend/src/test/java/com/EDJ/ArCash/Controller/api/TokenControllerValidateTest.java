package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.ValidationToken;
import com.EDJ.ArCash.Service.UserService;
import com.EDJ.ArCash.Service.ValidationTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Red minima de /api/auth/validate antes de tocar el flujo de activacion
 * (p. ej. usedToken duplicado en controller vs UserService).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TokenControllerValidateTest {

    private static final String TOKEN = "token-validacion-abc";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ValidationTokenService validationTokenService;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("Token valido: activa usuario; segundo uso del mismo token ya usado falla")
    void tokenValidoActivaYSegundoUsoFalla() throws Exception {
        User user = new User();
        user.setId(1L);
        ValidationToken validationToken = tokenPendiente(user);

        when(validationTokenService.buscarToken(TOKEN)).thenReturn(Optional.of(validationToken));
        // Simula el efecto de validarUsuario → usedToken (dueño de la activacion).
        doAnswer(inv -> {
            validationToken.setUsed(true);
            return null;
        }).when(userService).validarUsuario(user);

        mockMvc.perform(get("/api/auth/validate").param("token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("¡Cuenta verificada exitosamente! Ya puedes iniciar sesión."));

        mockMvc.perform(get("/api/auth/validate").param("token", TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "Este enlace de verificación ya fue utilizado. Tu cuenta ya está activada."));

        verify(userService, times(1)).validarUsuario(user);
    }

    private ValidationToken tokenPendiente(User user) {
        ValidationToken token = new ValidationToken();
        token.setToken(TOKEN);
        token.setUser(user);
        token.setExpirationDate(LocalDateTime.now().plusHours(1));
        token.setUsed(false);
        return token;
    }
}
