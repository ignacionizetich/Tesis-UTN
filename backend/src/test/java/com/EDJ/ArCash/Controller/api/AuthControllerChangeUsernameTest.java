package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Security.CustomUserDetails;
import com.EDJ.ArCash.Service.interfaces.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerChangeUsernameTest {

    private static final Long ID_AUTENTICADO = 42L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("changeUsername pasa a UserService el ID del principal autenticado, no otro")
    void pasaIdDelPrincipalAutenticado() throws Exception {
        when(userService.changeUsername(eq(ID_AUTENTICADO), eq("nuevoalias")))
                .thenReturn(com.EDJ.ArCash.Service.result.UsernameChangeResult.ok());

        mockMvc.perform(put("/api/auth/changeUsername")
                        .with(comoUsuarioAutenticado(ID_AUTENTICADO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newUsername\":\"nuevoalias\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).changeUsername(eq(ID_AUTENTICADO), eq("nuevoalias"));
        verifyNoMoreInteractions(userService);
    }

    private RequestPostProcessor comoUsuarioAutenticado(long userId) {
        CustomUserDetails principal = new CustomUserDetails(usuario(userId));
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private User usuario(long id) {
        User user = new User();
        user.setId(id);
        user.setPermissions(Permissions.USER);
        user.setAlias("ana.gomez");
        user.setCredentials(new Credentials(user, "ana.gomez", "irrelevante"));
        return user;
    }
}
