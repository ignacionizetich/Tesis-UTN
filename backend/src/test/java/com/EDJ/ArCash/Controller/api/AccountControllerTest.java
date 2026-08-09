package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.AuthDTO.AliasResponse;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Security.JwtUtils;
import com.EDJ.ArCash.Service.AccountService;
import com.EDJ.ArCash.Service.TransactionService;
import com.EDJ.ArCash.Service.UserService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de caracterizacion del contrato HTTP de /api/accounts.
 *
 * Igual que en favoritos, se corre con addFilters = false porque varios metodos
 * parsean el JWT por su cuenta y sus ramas de 401 quedarian inalcanzables detras
 * de la cadena de filtros real.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AccountControllerTest {

    private static final String TOKEN = "token-de-prueba";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final Long ID_USUARIO = 1L;
    private static final long ID_CUENTA_ARS = 10L;
    private static final long ID_CUENTA_USD = 20L;
    private static final String ALIAS_USUARIO = "ana.gomez";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        // Camino feliz de autenticacion, que cada test sobreescribe si necesita otra cosa.
        Claims claims = mock(Claims.class);
        when(claims.get("userID", String.class)).thenReturn(String.valueOf(ID_USUARIO));
        when(jwtUtils.getClaimJWT(TOKEN)).thenReturn(claims);
        // validateAccessToken devuelve ResponseEntity<?>, cuyo comodin impide usar thenReturn.
        doReturn(ResponseEntity.ok(ID_USUARIO)).when(jwtUtils).validateAccessToken(any());
    }

    // --- PUT /{id}/balance ---

    @Test
    @DisplayName("Ingresar un monto negativo devuelve 400 antes de mirar el token")
    void balanceConMontoNegativoDevuelve400() throws Exception {
        mockMvc.perform(put("/api/accounts/{id}/balance", ID_CUENTA_ARS)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("El monto a ingresar no puede ser negativo."))
                .andExpect(jsonPath("$.newBalance").value(-1.0));

        verify(jwtUtils, never()).validateAccessToken(any());
    }

    @Test
    @DisplayName("Con token invalido devuelve 498")
    void balanceConTokenInvalidoDevuelve498() throws Exception {
        doReturn(ResponseEntity.status(498).build()).when(jwtUtils).validateAccessToken(any());

        mockMvc.perform(put("/api/accounts/{id}/balance", ID_CUENTA_ARS)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":100}"))
                .andExpect(status().is(498))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Usuario invalido"));
    }

    @Test
    @DisplayName("Si la cuenta no existe devuelve 404")
    void balanceConCuentaInexistenteDevuelve404() throws Exception {
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/accounts/{id}/balance", ID_CUENTA_ARS)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":100}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La cuenta no existe."));
    }

    @Test
    @DisplayName("Si la cuenta es de otro usuario devuelve 498, no 403")
    void balanceSobreCuentaAjenaDevuelve498() throws Exception {
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.of(cuentaArs(usuario(99L))));

        mockMvc.perform(put("/api/accounts/{id}/balance", ID_CUENTA_ARS)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":100}"))
                .andExpect(status().is(498))
                .andExpect(jsonPath("$.message").value("El usuario no es propietario legitimo de la cuenta"));

        verify(accountService, never()).updateBalance(anyDouble(), anyLong());
    }

    @Test
    @DisplayName("Si el service no puede actualizar devuelve 404")
    void balanceQueFallaEnElServiceDevuelve404() throws Exception {
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.of(cuentaArs(usuario(ID_USUARIO))));
        when(accountService.updateBalance(100.0, ID_CUENTA_ARS)).thenReturn(false);

        mockMvc.perform(put("/api/accounts/{id}/balance", ID_CUENTA_ARS)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":100}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No se pudo actualizar el balance. Verifique el ID ingresado."));
    }

    @Test
    @DisplayName("El ingreso exitoso informa el saldo de la instancia leida antes de actualizar")
    void balanceExitosoDevuelveElSaldoPrevioAlIngreso() throws Exception {
        Account cuenta = cuentaArs(usuario(ID_USUARIO));
        cuenta.setBalance(500.0);
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.of(cuenta));
        when(accountService.updateBalance(100.0, ID_CUENTA_ARS)).thenReturn(true);

        // El controller nunca vuelve a consultar la cuenta despues de actualizarla:
        // devuelve el balance del objeto que ya tenia en memoria. Si la instancia no
        // es la misma que toca el service, el saldo informado se queda viejo.
        mockMvc.perform(put("/api/accounts/{id}/balance", ID_CUENTA_ARS)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ingreso de dinero realizado correctamente."))
                .andExpect(jsonPath("$.newBalance").value(500.0));
    }

    // --- GET /{id}/showBalance ---

    @Test
    @DisplayName("Sin header Authorization devuelve 401")
    void showBalanceSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/accounts/{id}/showBalance", ID_CUENTA_ARS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token no proporcionado o inválido"));
    }

    @Test
    @DisplayName("Devuelve balance, alias y CVU de la cuenta propia")
    void showBalanceDevuelveLosDatosDeLaCuenta() throws Exception {
        Account cuenta = cuentaArs(usuario(ID_USUARIO));
        cuenta.setBalance(1500.75);
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.of(cuenta));

        mockMvc.perform(get("/api/accounts/{id}/showBalance", ID_CUENTA_ARS)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1500.75))
                .andExpect(jsonPath("$.alias").value("MI.CUENTA.AA"))
                .andExpect(jsonPath("$.cvu").value("0000200112345678901234"));
    }

    @Test
    @DisplayName("Sobre una cuenta ajena o inexistente devuelve el mismo 403")
    void showBalanceDeCuentaAjenaDevuelve403() throws Exception {
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.of(cuentaArs(usuario(99L))));

        mockMvc.perform(get("/api/accounts/{id}/showBalance", ID_CUENTA_ARS)
                        .header("Authorization", BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("El usuario no es propietario de la cuenta"));

        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/accounts/{id}/showBalance", ID_CUENTA_ARS)
                        .header("Authorization", BEARER))
                .andExpect(status().isForbidden());
    }

    // --- PUT /{id}/changeAlias ---

    @Test
    @DisplayName("Cambiar alias con token invalido devuelve 498 sin llamar al service")
    void changeAliasConTokenInvalidoDevuelve498() throws Exception {
        doReturn(ResponseEntity.status(498).build()).when(jwtUtils).validateAccessToken(any());

        mockMvc.perform(put("/api/accounts/{id}/changeAlias", ID_CUENTA_ARS)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newAlias\":\"mi.alias.nuevo\"}"))
                .andExpect(status().is(498))
                .andExpect(jsonPath("$.message").value("Usuario invalido."));

        verify(accountService, never()).changeAlias(any(), anyLong(), any());
    }

    @Test
    @DisplayName("Cambiar alias delega en el service y devuelve su respuesta tal cual")
    void changeAliasDelegaEnElService() throws Exception {
        when(accountService.changeAlias(eq("mi.alias.nuevo"), eq(ID_CUENTA_ARS), any()))
                .thenReturn(ResponseEntity.ok(
                        AliasResponse.builder().success(true).message("Alias actualizado exitosamente.").build()));

        mockMvc.perform(put("/api/accounts/{id}/changeAlias", ID_CUENTA_ARS)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newAlias\":\"mi.alias.nuevo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Alias actualizado exitosamente."));
    }

    // --- GET /{id}/qr-data ---

    @Test
    @DisplayName("Los datos del QR incluyen el email, que no figura en la documentacion")
    void qrDataDevuelveLosDatosDelReceptor() throws Exception {
        User user = usuario(ID_USUARIO);
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.of(cuentaArs(user)));

        mockMvc.perform(get("/api/accounts/{id}/qr-data", ID_CUENTA_ARS)
                        .header("Authorization", BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletApp").value("ArCashV1"))
                .andExpect(jsonPath("$.accountId").value(ID_CUENTA_ARS))
                .andExpect(jsonPath("$.accountAlias").value("MI.CUENTA.AA"))
                .andExpect(jsonPath("$.receiverName").value("Ana Gomez"))
                .andExpect(jsonPath("$.dni").value("12345678"))
                .andExpect(jsonPath("$.email").value("ana@test.com"))
                .andExpect(jsonPath("$.currency").value("ARS"));
    }

    @Test
    @DisplayName("QR sin token devuelve 401")
    void qrDataSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/accounts/{id}/qr-data", ID_CUENTA_ARS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token no proporcionado o inválido"));
    }

    @Test
    @DisplayName("QR de una cuenta inexistente devuelve 404 y de una ajena 403")
    void qrDataDistingueCuentaInexistenteDeAjena() throws Exception {
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/accounts/{id}/qr-data", ID_CUENTA_ARS)
                        .header("Authorization", BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Cuenta no encontrada"));

        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.of(cuentaArs(usuario(99L))));

        mockMvc.perform(get("/api/accounts/{id}/qr-data", ID_CUENTA_ARS)
                        .header("Authorization", BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("El usuario no es propietario de la cuenta"));
    }

    // --- POST /usd ---

    @Test
    @DisplayName("Abrir cuenta en dolares busca al usuario por el nombre del principal")
    void abrirCuentaUsdDevuelveLosDatosDeLaCuentaNueva() throws Exception {
        User user = usuario(ID_USUARIO);
        when(userService.findUserByAlias(ALIAS_USUARIO)).thenReturn(Optional.of(user));
        when(accountService.openUsdAccount(user)).thenReturn(cuentaUsd(user));

        mockMvc.perform(post("/api/accounts/usd").principal(principalAutenticado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cuenta en dólares creada exitosamente"))
                .andExpect(jsonPath("$.accountId").value(ID_CUENTA_USD))
                .andExpect(jsonPath("$.accountAlias").value("MI.CUENTA.BB"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @DisplayName("Si no encuentra al usuario autenticado devuelve 404")
    void abrirCuentaUsdSinUsuarioDevuelve404() throws Exception {
        when(userService.findUserByAlias(ALIAS_USUARIO)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/accounts/usd").principal(principalAutenticado()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No se encontró el usuario autenticado"));
    }

    @Test
    @DisplayName("Tener ya una cuenta en dolares termina en 500 por el catch generico del controller")
    void abrirSegundaCuentaUsdDevuelve500() throws Exception {
        User user = usuario(ID_USUARIO);
        when(userService.findUserByAlias(ALIAS_USUARIO)).thenReturn(Optional.of(user));
        when(accountService.openUsdAccount(user))
                .thenThrow(new IllegalStateException("El usuario ya cuenta con una cuenta en dolares"));

        // El try/catch local se traga la IllegalStateException y la degrada a 500,
        // cuando semanticamente es un conflicto (409). Anotado en el backlog.
        mockMvc.perform(post("/api/accounts/usd").principal(principalAutenticado()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Error al crear cuenta en dólares: El usuario ya cuenta con una cuenta en dolares"));
    }

    // --- POST /{accountArsId}/buy-usd/{accountUsdId} ---

    @Test
    @DisplayName("Comprar dolares sin token devuelve 401")
    void comprarDolaresSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/accounts/{ars}/buy-usd/{usd}", ID_CUENTA_ARS, ID_CUENTA_USD)
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

        mockMvc.perform(post("/api/accounts/{ars}/buy-usd/{usd}", ID_CUENTA_ARS, ID_CUENTA_USD)
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

        mockMvc.perform(post("/api/accounts/{ars}/buy-usd/{usd}", ID_CUENTA_ARS, ID_CUENTA_USD)
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

        mockMvc.perform(post("/api/accounts/{ars}/buy-usd/{usd}", ID_CUENTA_ARS, ID_CUENTA_USD)
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
                .thenReturn(Map.of("success", false, "message", "Saldo insuficiente en cuenta en pesos"));

        // El error no viaja como BuyUsdResponse sino como el Map tal cual lo arma
        // el service: el contrato de la respuesta de error es distinto al del exito.
        mockMvc.perform(post("/api/accounts/{ars}/buy-usd/{usd}", ID_CUENTA_ARS, ID_CUENTA_USD)
                        .header("Authorization", BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountArs\":10000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Saldo insuficiente en cuenta en pesos"))
                .andExpect(jsonPath("$.amountUsd").doesNotExist());
    }

    // --- helpers ---

    /**
     * El endpoint recibe un Authentication como parametro, que Spring MVC resuelve
     * desde request.getUserPrincipal(). Ese principal lo publica un filtro de Spring
     * Security, asi que con addFilters = false hay que ponerlo a mano.
     */
    private Authentication principalAutenticado() {
        return new UsernamePasswordAuthenticationToken(ALIAS_USUARIO, "n/a", List.of());
    }

    private Map<String, Object> compraExitosa() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Compra de dólares exitosa");
        result.put("amountArs", 10000.0);
        result.put("amountUsd", 10.0);
        result.put("exchangeRate", 1000.0);
        result.put("taxAmount", 6000.0);
        result.put("taxPercentage", 60.0);
        result.put("totalDebitado", 16000.0);
        result.put("newBalanceArs", 5000.0);
        result.put("newBalanceUsd", 10.0);
        return result;
    }

    private User usuario(long id) {
        User user = new User();
        user.setId(id);
        user.setName("Ana");
        user.setLastName("Gomez");
        user.setDni("12345678");
        user.setEmail("ana@test.com");
        user.setAlias(ALIAS_USUARIO);
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

    private Account cuentaUsd(User propietario) {
        Account account = new Account();
        account.setIdAccount(ID_CUENTA_USD);
        account.setUser(propietario);
        account.setAccountType(Currency.USD);
        account.setAccountNickname("MI.CUENTA.BB");
        account.setAccountCvu("0000200112345678909876");
        return account;
    }
}
