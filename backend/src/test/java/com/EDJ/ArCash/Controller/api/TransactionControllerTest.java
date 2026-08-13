package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.AuthDTO.AccountSearchResponse;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Security.CustomUserDetails;
import com.EDJ.ArCash.Service.interfaces.AccountService;
import com.EDJ.ArCash.Service.result.BuyUsdResult;
import com.EDJ.ArCash.Service.result.OwnedBuyUsdResult;
import com.EDJ.ArCash.Service.result.OwnedSellUsdResult;
import com.EDJ.ArCash.Service.result.SellUsdResult;
import com.EDJ.ArCash.Service.interfaces.TransactionService;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        when(transactionService.buyUsdForOwner(ID_USUARIO, ID_CUENTA_ARS, ID_CUENTA_USD, 10000.0))
                .thenReturn(OwnedBuyUsdResult.arsNotFound());

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
        when(transactionService.buyUsdForOwner(ID_USUARIO, ID_CUENTA_ARS, ID_CUENTA_USD, 10000.0))
                .thenReturn(OwnedBuyUsdResult.forbidden());

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
        when(transactionService.buyUsdForOwner(ID_USUARIO, ID_CUENTA_ARS, ID_CUENTA_USD, 10000.0))
                .thenReturn(OwnedBuyUsdResult.ok(compraExitosa()));

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
        when(transactionService.buyUsdForOwner(ID_USUARIO, ID_CUENTA_ARS, ID_CUENTA_USD, 10000.0))
                .thenReturn(OwnedBuyUsdResult.fail(BuyUsdResult.fail("Saldo insuficiente en cuenta en pesos")));

        mockMvc.perform(post("/api/transactions/{ars}/buy-usd/{usd}", ID_CUENTA_ARS, ID_CUENTA_USD)
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountArs\":10000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Saldo insuficiente en cuenta en pesos"))
                .andExpect(jsonPath("$.amountUsd").doesNotExist());
    }

    @Test
    @DisplayName("Vender dolares sin principal devuelve 401 (rama preservada / filters off)")
    void venderDolaresSinTokenDevuelve401() throws Exception {
        TestSecurityContextHolder.clearContext();
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/api/transactions/{usd}/sell-usd/{ars}", ID_CUENTA_USD, ID_CUENTA_ARS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountUsd\":100}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token no proporcionado o inválido"));
    }

    @Test
    @DisplayName("Vender dolares con cuenta USD inexistente devuelve 404")
    void venderDolaresConCuentaInexistenteDevuelve404() throws Exception {
        when(transactionService.sellUsdForOwner(ID_USUARIO, ID_CUENTA_USD, ID_CUENTA_ARS, 100.0))
                .thenReturn(OwnedSellUsdResult.usdNotFound());

        mockMvc.perform(post("/api/transactions/{usd}/sell-usd/{ars}", ID_CUENTA_USD, ID_CUENTA_ARS)
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountUsd\":100}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cuenta en dólares no encontrada"));
    }

    @Test
    @DisplayName("Vender dolares desde una cuenta ajena devuelve 403")
    void venderDolaresDesdeCuentaAjenaDevuelve403() throws Exception {
        when(transactionService.sellUsdForOwner(ID_USUARIO, ID_CUENTA_USD, ID_CUENTA_ARS, 100.0))
                .thenReturn(OwnedSellUsdResult.forbidden());

        mockMvc.perform(post("/api/transactions/{usd}/sell-usd/{ars}", ID_CUENTA_USD, ID_CUENTA_ARS)
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountUsd\":100}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permiso para operar esta cuenta"));

        verify(transactionService, never()).sellUsd(anyLong(), anyLong(), anyDouble());
    }

    @Test
    @DisplayName("La venta exitosa devuelve el detalle completo de la conversion")
    void venderDolaresDevuelveElDetalleDeLaOperacion() throws Exception {
        when(transactionService.sellUsdForOwner(ID_USUARIO, ID_CUENTA_USD, ID_CUENTA_ARS, 100.0))
                .thenReturn(OwnedSellUsdResult.ok(ventaExitosa()));

        mockMvc.perform(post("/api/transactions/{usd}/sell-usd/{ars}", ID_CUENTA_USD, ID_CUENTA_ARS)
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountUsd\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Venta de dólares exitosa"))
                .andExpect(jsonPath("$.amountUsd").value(100.0))
                .andExpect(jsonPath("$.amountArs").value(100000.0))
                .andExpect(jsonPath("$.exchangeRate").value(1000.0))
                .andExpect(jsonPath("$.taxAmount").value(3.0))
                .andExpect(jsonPath("$.taxPercentage").value(3.0))
                .andExpect(jsonPath("$.totalDebitado").value(103.0))
                .andExpect(jsonPath("$.newBalanceArs").value(101000.0))
                .andExpect(jsonPath("$.newBalanceUsd").value(97.0));
    }

    @Test
    @DisplayName("Si la venta falla se devuelve el mapa minimo del service con 400")
    void venderDolaresQueFallaDevuelve400ConElMapaDelService() throws Exception {
        when(transactionService.sellUsdForOwner(ID_USUARIO, ID_CUENTA_USD, ID_CUENTA_ARS, 100.0))
                .thenReturn(OwnedSellUsdResult.fail(SellUsdResult.fail("Saldo insuficiente")));

        mockMvc.perform(post("/api/transactions/{usd}/sell-usd/{ars}", ID_CUENTA_USD, ID_CUENTA_ARS)
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountUsd\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Saldo insuficiente"))
                .andExpect(jsonPath("$.amountArs").doesNotExist());
    }

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

    private SellUsdResult ventaExitosa() {
        return SellUsdResult.ok(
                "Venta de dólares exitosa",
                100.0,
                100000.0,
                1000.0,
                3.0,
                3.0,
                103.0,
                101000.0,
                97.0
        );
    }

    @Test
    @DisplayName("Search por alias: DTO tipado con currency y user anidado")
    void searchPorAliasDevuelveDtoTipado() throws Exception {
        AccountSearchResponse dto = new AccountSearchResponse(
                ID_CUENTA_ARS,
                "MI.CUENTA.AA",
                "0000200112345678901234",
                "ARS",
                new AccountSearchResponse.UserSummary("Ana", "Gomez", "30111222")
        );
        when(accountService.searchByAliasOrCvu("MI.CUENTA.AA")).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/transactions/search/{input}", "MI.CUENTA.AA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idaccount").value(ID_CUENTA_ARS))
                .andExpect(jsonPath("$.alias").value("MI.CUENTA.AA"))
                .andExpect(jsonPath("$.cvu").value("0000200112345678901234"))
                .andExpect(jsonPath("$.currency").value("ARS"))
                .andExpect(jsonPath("$.user.nombre").value("Ana"))
                .andExpect(jsonPath("$.user.apellido").value("Gomez"))
                .andExpect(jsonPath("$.user.dni").value("30111222"));
    }

    @Test
    @DisplayName("Search por CVU si no hay alias: currency USD")
    void searchPorCvuCuandoNoHayAlias() throws Exception {
        AccountSearchResponse dto = new AccountSearchResponse(
                ID_CUENTA_USD,
                "MI.CUENTA.USD",
                "0000200199999999999999",
                "USD",
                new AccountSearchResponse.UserSummary("Ana", "Gomez", "30111222")
        );
        when(accountService.searchByAliasOrCvu("0000200199999999999999")).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/transactions/search/{input}", "0000200199999999999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idaccount").value(ID_CUENTA_USD))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.alias").value("MI.CUENTA.USD"));
    }

    @Test
    @DisplayName("Search sin resultado: 404")
    void searchSinResultado404() throws Exception {
        when(accountService.searchByAliasOrCvu("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/transactions/search/{input}", "ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Cuenta no encontrada."));
    }

    private User usuario(long id) {
        User user = new User();
        user.setId(id);
        user.setName("Ana");
        user.setLastName("Gomez");
        user.setDni("30111222");
        user.setPermissions(Permissions.USER);
        user.setCredentials(new Credentials(user, "ana.gomez", "irrelevante"));
        return user;
    }
}
