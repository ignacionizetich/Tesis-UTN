package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Security.JwtService;
import com.EDJ.ArCash.Service.AccountService;
import com.EDJ.ArCash.Service.BuyUsdResult;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de caracterizacion del contrato HTTP de /api/transactions.
 *
 * Por ahora cubre la compra de dolares, que se mudo desde /api/accounts.
 * Se corre con addFilters = false porque el controller parsea el JWT por su
 * cuenta y sus ramas de 401 quedarian inalcanzables detras de la cadena real.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TransactionControllerTest {

    private static final String TOKEN = "token-de-prueba";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final Long ID_USUARIO = 1L;
    private static final long ID_CUENTA_ARS = 10L;
    private static final long ID_CUENTA_USD = 20L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        Claims claims = mock(Claims.class);
        when(claims.get("userID", String.class)).thenReturn(String.valueOf(ID_USUARIO));
        when(jwtService.getClaimJWT(TOKEN)).thenReturn(claims);
    }

    @Test
    @DisplayName("Comprar dolares sin token devuelve 401")
    void comprarDolaresSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/transactions/{ars}/buy-usd/{usd}", ID_CUENTA_ARS, ID_CUENTA_USD)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountArs\":10000}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token no proporcionado o inválido"));
    }

    @Test
    @DisplayName("Comprar dolares con una cuenta en pesos inexistente devuelve 404")
    void comprarDolaresConCuentaInexistenteDevuelve404() throws Exception {
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/transactions/{ars}/buy-usd/{usd}", ID_CUENTA_ARS, ID_CUENTA_USD)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountArs\":10000}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cuenta en pesos no encontrada"));
    }

    @Test
    @DisplayName("Comprar dolares desde una cuenta ajena devuelve 403")
    void comprarDolaresDesdeCuentaAjenaDevuelve403() throws Exception {
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.of(cuentaArs(usuario(99L))));

        mockMvc.perform(post("/api/transactions/{ars}/buy-usd/{usd}", ID_CUENTA_ARS, ID_CUENTA_USD)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountArs\":10000}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permiso para operar esta cuenta"));

        verify(transactionService, never()).buyUsd(anyLong(), anyLong(), anyDouble());
    }

    @Test
    @DisplayName("La compra exitosa devuelve el detalle completo de la conversion")
    void comprarDolaresDevuelveElDetalleDeLaOperacion() throws Exception {
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.of(cuentaArs(usuario(ID_USUARIO))));
        when(transactionService.buyUsd(ID_CUENTA_ARS, ID_CUENTA_USD, 10000.0)).thenReturn(compraExitosa());

        mockMvc.perform(post("/api/transactions/{ars}/buy-usd/{usd}", ID_CUENTA_ARS, ID_CUENTA_USD)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountArs\":10000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Compra de dólares exitosa"))
                .andExpect(jsonPath("$.amountArs").value(10000.0))
                .andExpect(jsonPath("$.amountUsd").value(10.0))
                .andExpect(jsonPath("$.exchangeRate").value(1000.0))
                .andExpect(jsonPath("$.taxAmount").value(6000.0))
                .andExpect(jsonPath("$.taxPercentage").value(60.0))
                .andExpect(jsonPath("$.totalDebitado").value(16000.0))
                .andExpect(jsonPath("$.newBalanceArs").value(5000.0))
                .andExpect(jsonPath("$.newBalanceUsd").value(10.0));
    }

    @Test
    @DisplayName("Si la compra falla se devuelve el mapa crudo del service con 400")
    void comprarDolaresQueFallaDevuelve400ConElMapaDelService() throws Exception {
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.of(cuentaArs(usuario(ID_USUARIO))));
        when(transactionService.buyUsd(ID_CUENTA_ARS, ID_CUENTA_USD, 10000.0))
                .thenReturn(BuyUsdResult.fail("Saldo insuficiente en cuenta en pesos"));

        // El error viaja como map minimo {success, message}, no como BuyUsdResponse completo.
        mockMvc.perform(post("/api/transactions/{ars}/buy-usd/{usd}", ID_CUENTA_ARS, ID_CUENTA_USD)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountArs\":10000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Saldo insuficiente en cuenta en pesos"))
                .andExpect(jsonPath("$.amountUsd").doesNotExist());
    }

    // --- helpers ---

    private BuyUsdResult compraExitosa() {
        return BuyUsdResult.ok(
                "Compra de dólares exitosa",
                10000.0,
                10.0,
                1000.0,
                6000.0,
                60.0,
                16000.0,
                5000.0,
                10.0
        );
    }

    private User usuario(long id) {
        User user = new User();
        user.setId(id);
        user.setName("Ana");
        user.setLastName("Gomez");
        return user;
    }

    private Account cuentaArs(User propietario) {
        Account account = new Account();
        account.setIdAccount(ID_CUENTA_ARS);
        account.setUser(propietario);
        account.setAccountType(Currency.ARS);
        account.setAccountNickname("MI.CUENTA.AA");
        account.setAccountCvu("0000200112345678901234");
        return account;
    }
}
