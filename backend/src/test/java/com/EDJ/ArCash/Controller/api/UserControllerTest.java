package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.NonAuthDTO.RegistrerRequest;
import com.EDJ.ArCash.Service.result.RegisterResult;
import com.EDJ.ArCash.Service.result.RegistrationConflictCode;
import com.EDJ.ArCash.Service.result.RegistrationConflictMessages;
import com.EDJ.ArCash.Service.interfaces.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("Campos obligatorios faltantes: 400 Todos los campos son obligatorios")
    void camposObligatoriosFaltantes() throws Exception {
        when(userService.registerFromRequest(any(RegistrerRequest.class)))
                .thenReturn(RegisterResult.validation("Todos los campos son obligatorios."));

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":null,"lastName":"Gomez","dni":"1","email":null,"password":"x","alias":"a"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Todos los campos son obligatorios."));
    }

    @Test
    @DisplayName("Conflicto de email: mensaje ES singular")
    void conflictoEmail() throws Exception {
        when(userService.registerFromRequest(any(RegistrerRequest.class)))
                .thenReturn(RegisterResult.conflict(
                        RegistrationConflictMessages.format(List.of(RegistrationConflictCode.EMAIL_ALREADY_EXISTS))));

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("El email ya se encuentra en uso."));
    }

    @Test
    @DisplayName("Conflicto de alias: mensaje ES singular")
    void conflictoAlias() throws Exception {
        when(userService.registerFromRequest(any(RegistrerRequest.class)))
                .thenReturn(RegisterResult.conflict(
                        RegistrationConflictMessages.format(List.of(RegistrationConflictCode.ALIAS_ALREADY_EXISTS))));

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El nombre de usuario no está disponible."));
    }

    @Test
    @DisplayName("Conflicto de DNI: mensaje ES singular")
    void conflictoDni() throws Exception {
        when(userService.registerFromRequest(any(RegistrerRequest.class)))
                .thenReturn(RegisterResult.conflict(
                        RegistrationConflictMessages.format(List.of(RegistrationConflictCode.DNI_ALREADY_EXISTS))));

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El DNI ya está registrado."));
    }

    @Test
    @DisplayName("Dos conflictos: mensaje con 'y'")
    void dosConflictos() throws Exception {
        when(userService.registerFromRequest(any(RegistrerRequest.class)))
                .thenReturn(RegisterResult.conflict(RegistrationConflictMessages.format(List.of(
                        RegistrationConflictCode.EMAIL_ALREADY_EXISTS,
                        RegistrationConflictCode.ALIAS_ALREADY_EXISTS))));

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "El email ya se encuentra en uso y el nombre de usuario no está disponible."));
    }

    @Test
    @DisplayName("Tres conflictos: mensaje con comas y 'y'")
    void tresConflictos() throws Exception {
        when(userService.registerFromRequest(any(RegistrerRequest.class)))
                .thenReturn(RegisterResult.conflict(RegistrationConflictMessages.format(List.of(
                        RegistrationConflictCode.EMAIL_ALREADY_EXISTS,
                        RegistrationConflictCode.ALIAS_ALREADY_EXISTS,
                        RegistrationConflictCode.DNI_ALREADY_EXISTS))));

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "El email ya se encuentra en uso, el nombre de usuario no está disponible y el DNI ya está registrado."));
    }

    @Test
    @DisplayName("Registro exitoso: 200 y mensaje de activacion por email")
    void registroExitoso() throws Exception {
        when(userService.registerFromRequest(any(RegistrerRequest.class))).thenReturn(RegisterResult.ok());

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                        "Usuario registrado correctamente. Revisa tu email para activar tu cuenta."));
    }

    private String bodyValido() {
        return """
                {
                  "name":"Ana",
                  "lastName":"Gomez",
                  "dni":"30111222",
                  "email":"ana@test.com",
                  "password":"secreta",
                  "alias":"ana.gomez"
                }
                """;
    }
}
