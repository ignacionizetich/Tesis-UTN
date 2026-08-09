package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.ValidationTokenRepository;
import com.EDJ.ArCash.observer.Event;
import com.EDJ.ArCash.observer.EventPublisher;
import com.EDJ.ArCash.observer.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de caracterizacion: describen el comportamiento ACTUAL de AccountService.
 * Incluye el formato de los identificadores generados, que es lo mas delicado de
 * congelar porque queda persistido en la base.
 */
class AccountServiceTest {

    private static final long ID_USUARIO = 1L;
    private static final long ID_CUENTA = 10L;

    private AccountRepository accountRepository;
    private ValidationTokenRepository validationTokenRepository;
    private EventPublisher eventPublisher;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        validationTokenRepository = mock(ValidationTokenRepository.class);
        eventPublisher = mock(EventPublisher.class);
        accountService = new AccountService(accountRepository, validationTokenRepository, eventPublisher);
    }

    // --- alta de cuentas ---

    @Test
    @DisplayName("La cuenta nueva se crea en pesos, con saldo cero y publicando el evento")
    void createAccountCreaLaCuentaEnPesosConSaldoCero() {
        User user = usuario();

        accountService.createAccount(user);

        Account guardada = capturarCuentaGuardada();
        assertEquals(Currency.ARS, guardada.getAccountType());
        assertEquals(0.0, guardada.getBalance());
        assertEquals(user, guardada.getUser());

        Event evento = capturarEventoPublicado();
        assertEquals(EventType.ACCOUNT_CREATED, evento.getEventType());
        assertEquals(user, evento.getData("user"));
        assertEquals(guardada.getAccountNickname(), evento.getData("accountAlias"));
        assertEquals(guardada.getAccountCvu(), evento.getData("accountCvu"));
    }

    @Test
    @DisplayName("La cuenta en dolares se crea con su propio tipo de evento")
    void createUsdAccountCreaLaCuentaEnDolares() {
        User user = usuario();

        Account devuelta = accountService.createUsdAccount(user);

        Account guardada = capturarCuentaGuardada();
        assertEquals(Currency.USD, guardada.getAccountType());
        assertEquals(0.0, guardada.getBalance());
        assertEquals(guardada, devuelta);
        assertEquals(EventType.USD_ACCOUNT_CREATED, capturarEventoPublicado().getEventType());
    }

    @Test
    @DisplayName("No deja abrir una segunda cuenta en dolares")
    void openUsdAccountRechazaLaSegundaCuenta() {
        User user = usuario();
        when(accountRepository.existsByUserAndAccountType(user, Currency.USD)).thenReturn(true);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> accountService.openUsdAccount(user));

        assertEquals("El usuario ya cuenta con una cuenta en dolares", error.getMessage());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Abre la cuenta en dolares si el usuario todavia no tiene una")
    void openUsdAccountCreaLaCuentaSiNoExiste() {
        User user = usuario();
        when(accountRepository.existsByUserAndAccountType(user, Currency.USD)).thenReturn(false);

        Account cuenta = accountService.openUsdAccount(user);

        assertEquals(Currency.USD, cuenta.getAccountType());
        verify(accountRepository).save(any());
    }

    // --- identificadores generados ---

    @Test
    @DisplayName("El CVU generado tiene 22 digitos y arranca con el codigo de entidad")
    void elCvuGeneradoTieneElFormatoEsperado() {
        accountService.createAccount(usuario());

        String cvu = capturarCuentaGuardada().getAccountCvu();
        assertEquals(22, cvu.length());
        assertTrue(cvu.startsWith("00002001"), "el CVU deberia arrancar con el codigo de entidad: " + cvu);
        assertTrue(cvu.matches("\\d{22}"), "el CVU deberia ser solo digitos: " + cvu);
    }

    @Test
    @DisplayName("El alias generado son dos palabras y dos letras, en mayusculas")
    void elAliasGeneradoTieneElFormatoEsperado() {
        accountService.createAccount(usuario());

        String alias = capturarCuentaGuardada().getAccountNickname();
        assertTrue(alias.matches("[A-Z]+\\.[A-Z]+\\.[A-Z]{2}"),
                "el alias generado no tiene el formato esperado: " + alias);
        assertTrue(alias.length() <= 25);
    }

    @Test
    @DisplayName("Reintenta la generacion mientras el alias o el CVU ya existan")
    void reintentaLaGeneracionAnteColisiones() {
        when(accountRepository.existsByAccountNickname(anyString())).thenReturn(true, false);
        when(accountRepository.existsByAccountCvu(anyString())).thenReturn(true, false);

        accountService.createAccount(usuario());

        verify(accountRepository, times(2)).existsByAccountNickname(anyString());
        verify(accountRepository, times(2)).existsByAccountCvu(anyString());
    }

    // --- updateBalance ---

    @Test
    @DisplayName("El ingreso de dinero suma al saldo existente en vez de reemplazarlo")
    void updateBalanceSumaAlSaldoExistente() {
        Account cuenta = cuenta(usuario());
        cuenta.setBalance(100.0);
        when(accountRepository.findByIdAccount(ID_CUENTA)).thenReturn(Optional.of(cuenta));

        assertTrue(accountService.updateBalance(50.0, ID_CUENTA));

        assertEquals(150.0, cuenta.getBalance());
        verify(accountRepository).save(cuenta);
    }

    @Test
    @DisplayName("El servicio no valida el monto: un negativo descuenta saldo")
    void updateBalanceAceptaMontosNegativos() {
        Account cuenta = cuenta(usuario());
        cuenta.setBalance(100.0);
        when(accountRepository.findByIdAccount(ID_CUENTA)).thenReturn(Optional.of(cuenta));

        // La unica barrera contra montos negativos vive hoy en el controller.
        assertTrue(accountService.updateBalance(-30.0, ID_CUENTA));
        assertEquals(70.0, cuenta.getBalance());
    }

    @Test
    @DisplayName("Devuelve false si la cuenta no existe")
    void updateBalanceDevuelveFalseSiLaCuentaNoExiste() {
        when(accountRepository.findByIdAccount(ID_CUENTA)).thenReturn(Optional.empty());

        assertFalse(accountService.updateBalance(50.0, ID_CUENTA));
        verify(accountRepository, never()).save(any());
    }

    // --- changeAlias ---

    @Test
    @DisplayName("Rechaza un alias con formato invalido sin tocar la base")
    void changeAliasRechazaFormatoInvalido() {
        assertEquals(AliasChangeResult.FORMATO_INVALIDO,
                accountService.changeAlias("sinpunto", ID_CUENTA, ID_USUARIO));

        verify(accountRepository, never()).findByIdAccount(any());
    }

    @Test
    @DisplayName("Rechaza un alias que no tiene ninguna letra")
    void changeAliasRechazaAliasSoloNumerico() {
        assertEquals(AliasChangeResult.FORMATO_INVALIDO,
                accountService.changeAlias("123.456", ID_CUENTA, ID_USUARIO));
    }

    @Test
    @DisplayName("Rechaza un alias con dos puntos seguidos")
    void changeAliasRechazaPuntosConsecutivos() {
        assertEquals(AliasChangeResult.FORMATO_INVALIDO,
                accountService.changeAlias("mi..alias", ID_CUENTA, ID_USUARIO));
    }

    @Test
    @DisplayName("Informa que la cuenta no existe")
    void changeAliasInformaCuentaInexistente() {
        when(accountRepository.findByIdAccount(ID_CUENTA)).thenReturn(Optional.empty());

        assertEquals(AliasChangeResult.CUENTA_NO_ENCONTRADA,
                accountService.changeAlias("mi.alias.nuevo", ID_CUENTA, ID_USUARIO));
    }

    @Test
    @DisplayName("Informa que la cuenta es de otro usuario")
    void changeAliasInformaQueNoEsPropietario() {
        Account ajena = cuenta(usuario(99L));
        when(accountRepository.findByIdAccount(ID_CUENTA)).thenReturn(Optional.of(ajena));

        assertEquals(AliasChangeResult.NO_ES_PROPIETARIO,
                accountService.changeAlias("mi.alias.nuevo", ID_CUENTA, ID_USUARIO));

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Informa que el alias ya esta tomado")
    void changeAliasInformaAliasEnUso() {
        Account propia = cuenta(usuario());
        when(accountRepository.findByIdAccount(ID_CUENTA)).thenReturn(Optional.of(propia));
        when(accountRepository.existsByAccountNickname("mi.alias.nuevo")).thenReturn(true);

        assertEquals(AliasChangeResult.ALIAS_EN_USO,
                accountService.changeAlias("mi.alias.nuevo", ID_CUENTA, ID_USUARIO));

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cambia el alias y publica el evento correspondiente")
    void changeAliasActualizaYPublicaElEvento() {
        Account propia = cuenta(usuario());
        propia.setAccountNickname("ALIAS.VIEJO.AA");
        when(accountRepository.findByIdAccount(ID_CUENTA)).thenReturn(Optional.of(propia));
        when(accountRepository.existsByAccountNickname("mi.alias.nuevo")).thenReturn(false);

        assertEquals(AliasChangeResult.OK,
                accountService.changeAlias("mi.alias.nuevo", ID_CUENTA, ID_USUARIO));

        assertEquals("mi.alias.nuevo", propia.getAccountNickname());
        verify(accountRepository).save(propia);

        Event evento = capturarEventoPublicado();
        assertEquals(EventType.ALIAS_CHANGED, evento.getEventType());
        assertEquals("ALIAS.VIEJO.AA", evento.getData("oldAlias"));
        assertEquals("mi.alias.nuevo", evento.getData("newAlias"));
    }

    // --- helpers ---

    private Account capturarCuentaGuardada() {
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        return captor.getValue();
    }

    private Event capturarEventoPublicado() {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisher).publish(captor.capture());
        return captor.getValue();
    }

    private User usuario() {
        return usuario(ID_USUARIO);
    }

    private User usuario(long id) {
        User user = new User();
        user.setId(id);
        user.setName("Ana");
        user.setLastName("Gomez");
        return user;
    }

    private Account cuenta(User propietario) {
        Account account = new Account();
        account.setIdAccount(ID_CUENTA);
        account.setUser(propietario);
        account.setAccountType(Currency.ARS);
        return account;
    }
}
