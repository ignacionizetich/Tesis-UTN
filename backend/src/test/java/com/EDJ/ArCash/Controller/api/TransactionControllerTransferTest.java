package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Security.JwtService;
import com.EDJ.ArCash.Service.AccountService;
import com.EDJ.ArCash.Service.FavoriteContactService;
import com.EDJ.ArCash.Service.TransactionService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caracterizacion HTTP de POST /api/transactions/{id1}/transfer/{id2}.
 * Documenta el cableado actual (JWT parseado a mano) y el bug self-transfer→200.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TransactionControllerTransferTest {

    private static final String TOKEN = "token-de-prueba";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final Long ID_USUARIO = 1L;
    private static final long ID_ORIGEN = 10L;
    private static final long ID_DESTINO = 20L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private FavoriteContactService favoriteContactService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        Claims claims = mock(Claims.class);
        when(claims.get("userID", String.class)).thenReturn(String.valueOf(ID_USUARIO));
        when(jwtService.getClaimJWT(TOKEN)).thenReturn(claims);
        when(favoriteContactService.getFavoriteContactsByUser(ID_USUARIO)).thenReturn(List.of());
    }

    @Test
    @DisplayName("C1 Self-transfer: si el service devuelve success=true (aunque sea FAILED), HTTP 200")
    void selfTransferConSuccessTrueDevuelve200() throws Exception {
        when(accountService.findAccountByID(ID_ORIGEN)).thenReturn(Optional.of(cuenta(ID_ORIGEN)));
        // Mismo id en path origen y destino: el service (B3) devolveria success=true.
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        when(transactionService.transactionWithDetails(eq(ID_ORIGEN), eq(ID_ORIGEN), eq(100.0)))
                .thenReturn(result);

        mockMvc.perform(post("/api/transactions/{id1}/transfer/{id2}", ID_ORIGEN, ID_ORIGEN)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transferencia realizada correctamente"));

        verify(transactionService).transactionWithDetails(ID_ORIGEN, ID_ORIGEN, 100.0);
    }

    @Test
    @DisplayName("C2 Transfer: success=true → 200; success=false → 400 con message del map")
    void transferOkYFailContratoTipado() throws Exception {
        when(accountService.findAccountByID(ID_ORIGEN)).thenReturn(Optional.of(cuenta(ID_ORIGEN)));
        when(accountService.findAccountByID(ID_DESTINO)).thenReturn(Optional.of(cuenta(ID_DESTINO)));

        when(transactionService.transactionWithDetails(ID_ORIGEN, ID_DESTINO, 50.0))
                .thenReturn(Map.of("success", true));

        mockMvc.perform(post("/api/transactions/{id1}/transfer/{id2}", ID_ORIGEN, ID_DESTINO)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transferencia realizada correctamente"));

        when(transactionService.transactionWithDetails(ID_ORIGEN, ID_DESTINO, 999.0))
                .thenReturn(Map.of("success", false, "message", "Saldo insuficiente o error en la transacción"));

        mockMvc.perform(post("/api/transactions/{id1}/transfer/{id2}", ID_ORIGEN, ID_DESTINO)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Saldo insuficiente o error en la transacción"));
    }

    private Account cuenta(long id) {
        User user = new User();
        user.setId(ID_USUARIO);
        Account account = new Account();
        account.setIdAccount(id);
        account.setUser(user);
        account.setAccountType(Currency.ARS);
        return account;
    }
}
