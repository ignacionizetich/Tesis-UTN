package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caracterizacion HTTP de POST /api/user/create (mensajes ES de conflictos).
 */
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
        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":null,"lastName":"Gomez","dni":"1","email":null,"password":"x","alias":"a"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.mensaje").value("Todos los campos son obligatorios."));

        verify(userService, never()).insertarUsuario(any(), anyString());
    }

    @Test
    @DisplayName("Conflicto de email: mensaje ES singular")
    void conflictoEmail() throws Exception {
        doThrow(new RuntimeException("EMAIL_ALREADY_EXISTS"))
                .when(userService).insertarUsuario(any(User.class), anyString());

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.mensaje").value("El email ya se encuentra en uso."));
    }

    @Test
    @DisplayName("Conflicto de alias: mensaje ES singular")
    void conflictoAlias() throws Exception {
        doThrow(new RuntimeException("ALIAS_ALREADY_EXISTS"))
                .when(userService).insertarUsuario(any(User.class), anyString());

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El nombre de usuario no está disponible."));
    }

    @Test
    @DisplayName("Conflicto de DNI: mensaje ES singular")
    void conflictoDni() throws Exception {
        doThrow(new RuntimeException("DNI_ALREADY_EXISTS"))
                .when(userService).insertarUsuario(any(User.class), anyString());

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El DNI ya está registrado."));
    }

    @Test
    @DisplayName("Dos conflictos: mensaje con 'y'")
    void dosConflictos() throws Exception {
        doThrow(new RuntimeException("EMAIL_ALREADY_EXISTS,ALIAS_ALREADY_EXISTS"))
                .when(userService).insertarUsuario(any(User.class), anyString());

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "El email ya se encuentra en uso y el nombre de usuario no está disponible."));
    }

    @Test
    @DisplayName("Tres conflictos: mensaje con comas y 'y'")
    void tresConflictos() throws Exception {
        doThrow(new RuntimeException("EMAIL_ALREADY_EXISTS,ALIAS_ALREADY_EXISTS,DNI_ALREADY_EXISTS"))
                .when(userService).insertarUsuario(any(User.class), anyString());

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "El email ya se encuentra en uso, el nombre de usuario no está disponible y el DNI ya está registrado."));
    }

    @Test
    @DisplayName("Registro exitoso: 200 y mensaje de activacion por email")
    void registroExitoso() throws Exception {
        doNothing().when(userService).insertarUsuario(any(User.class), eq("secreta"));

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.mensaje").value(
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
