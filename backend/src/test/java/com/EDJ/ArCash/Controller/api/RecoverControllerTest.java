package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Service.AuthService;
import com.EDJ.ArCash.Service.CredentialsService;
import com.EDJ.ArCash.Service.RecoveryTokenValidationResult;
import com.EDJ.ArCash.Service.ResetPasswordResult;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caracterizacion HTTP de RecoverController (validate-recovery-token + reset-password).
 * Mapping por Kind tipado (sin heuristica de substrings en el controller).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecoverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CredentialsService credentialsService;

    @Test
    @DisplayName("Token de recuperacion valido: 200 valid=true")
    void validateTokenValido() throws Exception {
        when(authService.validateRecoveryToken("tok-ok")).thenReturn(RecoveryTokenValidationResult.valid());

        mockMvc.perform(get("/api/auth/validate-recovery-token").param("token", "tok-ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("Enlace de recuperación válido"));
    }

    @Test
    @DisplayName("Token de recuperacion invalido: 401 valid=false")
    void validateTokenInvalido() throws Exception {
        when(authService.validateRecoveryToken("tok-bad")).thenReturn(RecoveryTokenValidationResult.invalid());

        mockMvc.perform(get("/api/auth/validate-recovery-token").param("token", "tok-bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value(
                        "El enlace de recuperación es inválido, ha expirado o ya fue utilizado"));
    }

    @Test
    @DisplayName("Excepcion al validar token: 500 valid=false")
    void validateTokenLanzaExcepcion() throws Exception {
        when(authService.validateRecoveryToken("tok-err")).thenReturn(RecoveryTokenValidationResult.error());

        mockMvc.perform(get("/api/auth/validate-recovery-token").param("token", "tok-err"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Error al validar el enlace de recuperación"));
    }

    @Test
    @DisplayName("Reset exitoso: OK → 200")
    void resetExitoso() throws Exception {
        when(credentialsService.actualizarPassword("tok", "a", "a"))
                .thenReturn(ResetPasswordResult.ok(
                        "¡Contraseña actualizada exitosamente! Ya puedes iniciar sesión con tu nueva contraseña."));

        mockMvc.perform(post("/api/auth/reset-password")
                        .param("token", "tok")
                        .param("password", "a")
                        .param("confirmPassword", "a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "¡Contraseña actualizada exitosamente! Ya puedes iniciar sesión con tu nueva contraseña."));
    }

    @Test
    @DisplayName("Reset token invalido: UNAUTHORIZED → 401")
    void resetTokenInvalidoDevuelve401() throws Exception {
        when(credentialsService.actualizarPassword("tok", "a", "a"))
                .thenReturn(ResetPasswordResult.unauthorized(
                        "El enlace de recuperación no es válido o no existe."));

        mockMvc.perform(post("/api/auth/reset-password")
                        .param("token", "tok")
                        .param("password", "a")
                        .param("confirmPassword", "a"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "El enlace de recuperación no es válido o no existe."));
    }

    @Test
    @DisplayName("Reset token ya usado: UNAUTHORIZED → 401")
    void resetTokenYaUsadoDevuelve401() throws Exception {
        when(credentialsService.actualizarPassword("tok", "a", "a"))
                .thenReturn(ResetPasswordResult.unauthorized(
                        "Este enlace de recuperación ya fue utilizado. Solicita un nuevo enlace si necesitas cambiar tu contraseña nuevamente."));

        mockMvc.perform(post("/api/auth/reset-password")
                        .param("token", "tok")
                        .param("password", "a")
                        .param("confirmPassword", "a"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Reset token expirado: UNAUTHORIZED → 401")
    void resetTokenExpiradoDevuelve401() throws Exception {
        when(credentialsService.actualizarPassword("tok", "a", "a"))
                .thenReturn(ResetPasswordResult.unauthorized(
                        "El enlace de recuperación ha expirado. Solicita un nuevo enlace para restablecer tu contraseña."));

        mockMvc.perform(post("/api/auth/reset-password")
                        .param("token", "tok")
                        .param("password", "a")
                        .param("confirmPassword", "a"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Reset contraseñas no coinciden: BAD_REQUEST → 400")
    void resetPasswordsNoCoincidenDevuelve400() throws Exception {
        when(credentialsService.actualizarPassword("tok", "a", "b"))
                .thenReturn(ResetPasswordResult.badRequest(
                        "Las contraseñas no coinciden. Verifica que ambas sean iguales."));

        mockMvc.perform(post("/api/auth/reset-password")
                        .param("token", "tok")
                        .param("password", "a")
                        .param("confirmPassword", "b"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "Las contraseñas no coinciden. Verifica que ambas sean iguales."));
    }

    @Test
    @DisplayName("Reset excepcion: 500 mensaje fijo")
    void resetExcepcionDevuelve500() throws Exception {
        when(credentialsService.actualizarPassword("tok", "a", "a"))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/auth/reset-password")
                        .param("token", "tok")
                        .param("password", "a")
                        .param("confirmPassword", "a"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "Error interno del servidor. Por favor, inténtalo de nuevo."));
    }
}
