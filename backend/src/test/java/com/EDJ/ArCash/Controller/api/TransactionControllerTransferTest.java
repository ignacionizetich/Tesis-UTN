package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Security.CustomUserDetails;
import com.EDJ.ArCash.Service.AccountService;
import com.EDJ.ArCash.Service.OwnedTransferResult;
import com.EDJ.ArCash.Service.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caracterizacion HTTP de transfer. addFilters=false + AuthenticationPrincipal
 * (misma identidad que publicaria el filtro JWT).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TransactionControllerTransferTest {

    private static final Long ID_USUARIO = 1L;
    private static final long ID_ORIGEN = 10L;
    private static final long ID_DESTINO = 20L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private TransactionService transactionService;

    @AfterEach
    void limpiarSecurityContext() {
        TestSecurityContextHolder.clearContext();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("C1 Self-transfer: si el service devuelve OK, HTTP 200")
    void selfTransferConSuccessTrueDevuelve200() throws Exception {
        when(transactionService.transferForOwner(ID_USUARIO, ID_ORIGEN, ID_ORIGEN, 100.0))
                .thenReturn(OwnedTransferResult.ok());

        mockMvc.perform(post("/api/transactions/{id1}/transfer/{id2}", ID_ORIGEN, ID_ORIGEN)
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transferencia realizada correctamente"));

        verify(transactionService).transferForOwner(ID_USUARIO, ID_ORIGEN, ID_ORIGEN, 100.0);
    }

    @Test
    @DisplayName("C2 Transfer: OK → 200; FAIL → 400 con message del result")
    void transferOkYFailContratoTipado() throws Exception {
        when(transactionService.transferForOwner(ID_USUARIO, ID_ORIGEN, ID_DESTINO, 50.0))
                .thenReturn(OwnedTransferResult.ok());

        mockMvc.perform(post("/api/transactions/{id1}/transfer/{id2}", ID_ORIGEN, ID_DESTINO)
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transferencia realizada correctamente"));

        when(transactionService.transferForOwner(ID_USUARIO, ID_ORIGEN, ID_DESTINO, 999.0))
                .thenReturn(OwnedTransferResult.fail("Saldo insuficiente o error en la transacción"));

        mockMvc.perform(post("/api/transactions/{id1}/transfer/{id2}", ID_ORIGEN, ID_DESTINO)
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Saldo insuficiente o error en la transacción"));
    }

    private RequestPostProcessor comoUsuarioAutenticado() {
        CustomUserDetails principal = new CustomUserDetails(usuario());
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return request -> {
            TestSecurityContextHolder.setAuthentication(auth);
            SecurityContextHolder.getContext().setAuthentication(auth);
            return request;
        };
    }

    private User usuario() {
        User user = new User();
        user.setId(ID_USUARIO);
        user.setPermissions(Permissions.USER);
        user.setCredentials(new Credentials(user, "ana.gomez", "irrelevante"));
        return user;
    }
}
