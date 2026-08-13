package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Security.CustomUserDetails;
import com.EDJ.ArCash.Service.result.UserDataView;
import com.EDJ.ArCash.Service.interfaces.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserDataControllerTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_CUENTA = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("Usuario con cuenta ARS: shape UserDTO completo")
    void datosUsuarioConCuenta() throws Exception {
        when(userService.getUserData(any(User.class))).thenReturn(Optional.of(new UserDataView(
                "Ana", "Gomez", "30111222", "ana@test.com",
                "ana.gomez", "MI.CUENTA.AA", ID_CUENTA, "0000200112345678901234", 1500.5
        )));

        mockMvc.perform(get("/api/user/data").with(comoUsuarioAutenticado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana"))
                .andExpect(jsonPath("$.lastName").value("Gomez"))
                .andExpect(jsonPath("$.dni").value("30111222"))
                .andExpect(jsonPath("$.email").value("ana@test.com"))
                .andExpect(jsonPath("$.username").value("ana.gomez"))
                .andExpect(jsonPath("$.alias").value("MI.CUENTA.AA"))
                .andExpect(jsonPath("$.idAccount").value(ID_CUENTA))
                .andExpect(jsonPath("$.cvu").value("0000200112345678901234"))
                .andExpect(jsonPath("$.balance").value(1500.5));
    }

    @Test
    @DisplayName("Usuario sin cuenta: 404 con error fijo")
    void sinCuentaDevuelve404() throws Exception {
        when(userService.getUserData(any(User.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/user/data").with(comoUsuarioAutenticado()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Cuenta no encontrada para el usuario"));
    }

    @Test
    @DisplayName("Sin autenticacion: 401")
    void sinAuthDevuelve401() throws Exception {
        mockMvc.perform(get("/api/user/data"))
                .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor comoUsuarioAutenticado() {
        CustomUserDetails principal = new CustomUserDetails(usuario());
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private User usuario() {
        User user = new User();
        user.setId(ID_USUARIO);
        user.setName("Ana");
        user.setLastName("Gomez");
        user.setDni("30111222");
        user.setEmail("ana@test.com");
        user.setAlias("ana.gomez");
        user.setPermissions(Permissions.USER);
        user.setCredentials(new Credentials(user, "ana.gomez", "irrelevante"));
        return user;
    }
}
