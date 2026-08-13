package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Service.result.ResendEmailResult;
import com.EDJ.ArCash.Service.interfaces.AuthService;
import com.EDJ.ArCash.Service.interfaces.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResendControllerTest {

    private static final String VALIDATION_MESSAGE =
            "Si el email corresponde a una cuenta pendiente de validación, te enviamos un nuevo enlace.";
    private static final String PASSWORD_RECOVERY_MESSAGE =
            "Si el email está registrado, te enviamos un enlace de recuperación.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("Validation: email enviado → 200 mensaje generico")
    void validationEnviadoDevuelveMensajeGenerico() throws Exception {
        when(userService.resendValidationEmailRequest("ok@mail.com"))
                .thenReturn(ResendEmailResult.ok(VALIDATION_MESSAGE));

        mockMvc.perform(post("/api/resend/validation").param("email", "ok@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(VALIDATION_MESSAGE));

        verify(userService).resendValidationEmailRequest("ok@mail.com");
    }

    @Test
    @DisplayName("Validation: email inexistente o ya validado → mismo 200 (anti-enum)")
    void validationFallidoTambienDevuelveMensajeGenerico() throws Exception {
        when(userService.resendValidationEmailRequest(anyString()))
                .thenReturn(ResendEmailResult.ok(VALIDATION_MESSAGE));

        mockMvc.perform(post("/api/resend/validation").param("email", "ghost@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(VALIDATION_MESSAGE));
    }

    @Test
    @DisplayName("Validation: email vacio → 400")
    void validationEmailVacio() throws Exception {
        when(userService.resendValidationEmailRequest(anyString()))
                .thenReturn(ResendEmailResult.emailRequired());

        mockMvc.perform(post("/api/resend/validation").param("email", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("El email es requerido."));
    }

    @Test
    @DisplayName("Password recovery: inexistente → mismo 200 (anti-enum)")
    void passwordRecoveryFallidoTambienDevuelveMensajeGenerico() throws Exception {
        when(authService.resendPasswordRecoveryEmail(anyString()))
                .thenReturn(ResendEmailResult.ok(PASSWORD_RECOVERY_MESSAGE));

        mockMvc.perform(post("/api/resend/password-recovery").param("email", "ghost@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(PASSWORD_RECOVERY_MESSAGE));
    }

    @Test
    @DisplayName("Password recovery: enviado → 200 mensaje generico")
    void passwordRecoveryEnviadoDevuelveMensajeGenerico() throws Exception {
        when(authService.resendPasswordRecoveryEmail("ok@mail.com"))
                .thenReturn(ResendEmailResult.ok(PASSWORD_RECOVERY_MESSAGE));

        mockMvc.perform(post("/api/resend/password-recovery").param("email", "ok@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(PASSWORD_RECOVERY_MESSAGE));
    }
}
