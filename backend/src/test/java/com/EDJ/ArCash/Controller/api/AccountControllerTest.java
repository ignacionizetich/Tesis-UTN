package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Security.CustomUserDetails;
import com.EDJ.ArCash.Service.AccountService;
import com.EDJ.ArCash.Service.AliasChangeResult;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de caracterizacion del contrato HTTP de /api/accounts.
 *
 * Desde que la identidad sale del SecurityContext, estos tests corren con la
 * cadena de filtros real y publican el principal con un post-processor, igual
 * que haria el filtro JWT en produccion. El rechazo de las peticiones anonimas
 * lo cubre AccountControllerSecurityTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerTest {

    private static final Long ID_USUARIO = 1L;
    private static final long ID_CUENTA_ARS = 10L;
    private static final long ID_CUENTA_USD = 20L;
    private static final String ALIAS_USUARIO = "ana.gomez";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    // --- PUT /{id}/balance ---

    @Test
    @DisplayName("Ingresar un monto negativo devuelve 400")
    void balanceConMontoNegativoDevuelve400() throws Exception {
        mockMvc.perform(put("/api/accounts/{id}/balance", ID_CUENTA_ARS)
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("El monto a ingresar no puede ser negativo."))
                .andExpect(jsonPath("$.newBalance").value(-1.0));
    }

    @Test
    @DisplayName("Si la cuenta no existe devuelve 404")
    void balanceConCuentaInexistenteDevuelve404() throws Exception {
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/accounts/{id}/balance", ID_CUENTA_ARS)
                        .with(comoUsuarioAutenticado())
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
                        .with(comoUsuarioAutenticado())
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
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":100}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No se pudo actualizar el balance. Verifique el ID ingresado."));
    }

    @Test
    @DisplayName("El ingreso exitoso informa el saldo actualizado tras el update")
    void balanceExitosoDevuelveElSaldoActualizado() throws Exception {
        Account cuentaAntes = cuentaArs(usuario(ID_USUARIO));
        cuentaAntes.setBalance(500.0);
        Account cuentaDespues = cuentaArs(usuario(ID_USUARIO));
        cuentaDespues.setBalance(600.0);
        when(accountService.findAccountByID(ID_CUENTA_ARS))
                .thenReturn(Optional.of(cuentaAntes))
                .thenReturn(Optional.of(cuentaDespues));
        when(accountService.updateBalance(100.0, ID_CUENTA_ARS)).thenReturn(true);

        mockMvc.perform(put("/api/accounts/{id}/balance", ID_CUENTA_ARS)
                        .with(comoUsuarioAutenticado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"balance\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ingreso de dinero realizado correctamente."))
                .andExpect(jsonPath("$.newBalance").value(600.0));
    }

    // --- GET /{id}/showBalance ---

    @Test
    @DisplayName("Devuelve balance, alias y CVU de la cuenta propia")
    void showBalanceDevuelveLosDatosDeLaCuenta() throws Exception {
        Account cuenta = cuentaArs(usuario(ID_USUARIO));
        cuenta.setBalance(1500.75);
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.of(cuenta));

        mockMvc.perform(get("/api/accounts/{id}/showBalance", ID_CUENTA_ARS)
                        .with(comoUsuarioAutenticado()))
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
                        .with(comoUsuarioAutenticado()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("El usuario no es propietario de la cuenta"));

        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/accounts/{id}/showBalance", ID_CUENTA_ARS)
                        .with(comoUsuarioAutenticado()))
                .andExpect(status().isForbidden());
    }

    // --- PUT /{id}/changeAlias ---

    @Test
    @DisplayName("El cambio de alias exitoso devuelve 200")
    void changeAliasExitosoDevuelve200() throws Exception {
        cambiarAliasDevuelve(AliasChangeResult.OK)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Alias actualizado exitosamente."));
    }

    @Test
    @DisplayName("Un alias mal formado devuelve 400")
    void changeAliasConFormatoInvalidoDevuelve400() throws Exception {
        cambiarAliasDevuelve(AliasChangeResult.FORMATO_INVALIDO)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Formato de alias inválido. Debe tener entre 4 y 25 caracteres, solo letras, números y puntos, al menos un punto en el medio, no puede ser solo números ni tener '..'."));
    }

    @Test
    @DisplayName("Una cuenta inexistente devuelve 498, no 404")
    void changeAliasSobreCuentaInexistenteDevuelve498() throws Exception {
        cambiarAliasDevuelve(AliasChangeResult.CUENTA_NO_ENCONTRADA)
                .andExpect(status().is(498))
                .andExpect(jsonPath("$.message").value("Cuenta no encontrada."));
    }

    @Test
    @DisplayName("Una cuenta ajena devuelve 403")
    void changeAliasSobreCuentaAjenaDevuelve403() throws Exception {
        cambiarAliasDevuelve(AliasChangeResult.NO_ES_PROPIETARIO)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes permisos para hacer eso."));
    }

    @Test
    @DisplayName("Un alias ya tomado devuelve 403")
    void changeAliasConAliasEnUsoDevuelve403() throws Exception {
        cambiarAliasDevuelve(AliasChangeResult.ALIAS_EN_USO)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Alias actualmente en uso."));
    }

    // --- GET /{id}/qr-data ---

    @Test
    @DisplayName("Los datos del QR incluyen el email, que no figura en la documentacion")
    void qrDataDevuelveLosDatosDelReceptor() throws Exception {
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.of(cuentaArs(usuario(ID_USUARIO))));

        mockMvc.perform(get("/api/accounts/{id}/qr-data", ID_CUENTA_ARS)
                        .with(comoUsuarioAutenticado()))
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
    @DisplayName("QR de una cuenta inexistente devuelve 404 y de una ajena 403")
    void qrDataDistingueCuentaInexistenteDeAjena() throws Exception {
        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/accounts/{id}/qr-data", ID_CUENTA_ARS)
                        .with(comoUsuarioAutenticado()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Cuenta no encontrada"));

        when(accountService.findAccountByID(ID_CUENTA_ARS)).thenReturn(Optional.of(cuentaArs(usuario(99L))));

        mockMvc.perform(get("/api/accounts/{id}/qr-data", ID_CUENTA_ARS)
                        .with(comoUsuarioAutenticado()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("El usuario no es propietario de la cuenta"));
    }

    // --- GET /user-accounts ---

    @Test
    @DisplayName("Lista las cuentas del usuario con el id como texto")
    void listaLasCuentasDelUsuario() throws Exception {
        User user = usuario(ID_USUARIO);
        Account enPesos = cuentaArs(user);
        enPesos.setBalance(1500.75);
        Account enDolares = cuentaUsd(user);
        enDolares.setBalance(20.5);
        when(accountService.findAccountsByUser(ID_USUARIO)).thenReturn(List.of(enPesos, enDolares));

        mockMvc.perform(get("/api/accounts/user-accounts").with(comoUsuarioAutenticado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.accounts.length()").value(2))
                .andExpect(jsonPath("$.accounts[0].id").value(String.valueOf(ID_CUENTA_ARS)))
                .andExpect(jsonPath("$.accounts[0].balance").value(1500.75))
                .andExpect(jsonPath("$.accounts[0].alias").value("MI.CUENTA.AA"))
                .andExpect(jsonPath("$.accounts[0].cvu").value("0000200112345678901234"))
                .andExpect(jsonPath("$.accounts[0].currency").value("ARS"))
                .andExpect(jsonPath("$.accounts[1].id").value(String.valueOf(ID_CUENTA_USD)))
                .andExpect(jsonPath("$.accounts[1].balance").value(20.5))
                .andExpect(jsonPath("$.accounts[1].currency").value("USD"));
    }

    @Test
    @DisplayName("Un usuario sin cuentas recibe una lista vacia, no un error")
    void listaVaciaSiElUsuarioNoTieneCuentas() throws Exception {
        when(accountService.findAccountsByUser(ID_USUARIO)).thenReturn(List.of());

        mockMvc.perform(get("/api/accounts/user-accounts").with(comoUsuarioAutenticado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.accounts.length()").value(0));
    }

    // --- POST /usd ---

    @Test
    @DisplayName("Abrir cuenta en dolares usa el usuario del principal")
    void abrirCuentaUsdDevuelveLosDatosDeLaCuentaNueva() throws Exception {
        when(accountService.openUsdAccount(any(User.class)))
                .thenAnswer(invocacion -> cuentaUsd(invocacion.getArgument(0)));

        mockMvc.perform(post("/api/accounts/usd").with(comoUsuarioAutenticado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cuenta en dólares creada exitosamente"))
                .andExpect(jsonPath("$.accountId").value(ID_CUENTA_USD))
                .andExpect(jsonPath("$.accountAlias").value("MI.CUENTA.BB"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @DisplayName("Tener ya una cuenta en dolares termina en 409 (conflicto)")
    void abrirSegundaCuentaUsdDevuelve409() throws Exception {
        when(accountService.openUsdAccount(any(User.class)))
                .thenThrow(new IllegalStateException("El usuario ya cuenta con una cuenta en dolares"));

        mockMvc.perform(post("/api/accounts/usd").with(comoUsuarioAutenticado()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("El usuario ya cuenta con una cuenta en dolares"));
    }

    // --- helpers ---

    private ResultActions cambiarAliasDevuelve(AliasChangeResult resultado) throws Exception {
        when(accountService.changeAlias(eq("mi.alias.nuevo"), eq(ID_CUENTA_ARS), eq(ID_USUARIO)))
                .thenReturn(resultado);

        return mockMvc.perform(put("/api/accounts/{id}/changeAlias", ID_CUENTA_ARS)
                .with(comoUsuarioAutenticado())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newAlias\":\"mi.alias.nuevo\"}"));
    }

    /**
     * Publica en el SecurityContext el mismo principal que arma el filtro JWT en
     * produccion: un CustomUserDetails con la entidad User ya cargada.
     */
    private RequestPostProcessor comoUsuarioAutenticado() {
        CustomUserDetails principal = new CustomUserDetails(usuario(ID_USUARIO));
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private User usuario(long id) {
        User user = new User();
        user.setId(id);
        user.setName("Ana");
        user.setLastName("Gomez");
        user.setDni("12345678");
        user.setEmail("ana@test.com");
        user.setAlias(ALIAS_USUARIO);
        // CustomUserDetails saca el username y la password de las credenciales.
        user.setCredentials(new Credentials(user, ALIAS_USUARIO, "irrelevante"));
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
