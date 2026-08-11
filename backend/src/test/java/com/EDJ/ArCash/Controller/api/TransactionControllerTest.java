package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Security.CustomUserDetails;
import com.EDJ.ArCash.Service.AccountService;
import com.EDJ.ArCash.Service.BuyUsdResult;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP de buy-usd. addFilters=false permite ejercer la rama
 * principal==null (inalcanzable en produccion tras anyRequest().authenticated()).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TransactionControllerTest {

    private static final Long ID_USUARIO = 1L;
    private static final long ID_CUENTA_ARS = 10L;
    private static final long ID_CUENTA_USD = 20L;

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
    @DisplayName("Comprar dolares sin principal devuelve 401 (rama preservada / filters off)")
    void comprarDolaresSinTokenDevuelve401() throws Exception {
        TestSecurityContextHolder.clearContext();
        SecurityContextHolder.clearContext();

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
                        .with(comoUsuarioAutenticado())
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
                        .with(comoUsuarioAutenticado())
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
                        .with(comoUsuarioAutenticado())
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
    @DisplayName("Si la compra falla se devuelve el mapa minimo del service con 400")
    void comprarDolaresQueFallaDevuelve400ConElMapaDelService() throws Exception {
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.of(cuentaArs(usuario(ID_USUARIO))));
        when(transactionService.buyUsd(ID_CUENTA_ARS, ID_CUENTA_USD, 10000.0))
                .thenReturn(BuyUsdResult.fail("Saldo insuficiente en cuenta en pesos"));

        mockMvc.perform(post("/api/transactions/{ars}/buy-usd/{usd}", ID_CUENTA_ARS, ID_CUENTA_USD)
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountArs\":10000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Saldo insuficiente en cuenta en pesos"))
                .andExpect(jsonPath("$.amountUsd").doesNotExist());
    }

    /**
     * Con addFilters=false hay que publicar el principal en SecurityContextHolder
     * a mano: el filtro de test de Spring Security no corre.
     */
    private RequestPostProcessor comoUsuarioAutenticado() {
        CustomUserDetails principal = new CustomUserDetails(usuario(ID_USUARIO));
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return request -> {
            TestSecurityContextHolder.setAuthentication(auth);
            SecurityContextHolder.getContext().setAuthentication(auth);
            return request;
        };
    }

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
        user.setPermissions(Permissions.USER);
        user.setCredentials(new Credentials(user, "ana.gomez", "irrelevante"));
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
