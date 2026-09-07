package com.EDJ.ArCash.Service;
import com.EDJ.ArCash.Service.result.AliasChangeResult;
import com.EDJ.ArCash.Service.result.DepositResult;
import com.EDJ.ArCash.Service.result.OpenUsdResult;
import com.EDJ.ArCash.Service.support.AccountIdentifierGenerator;
import com.EDJ.ArCash.Service.support.AliasFormatValidator;
import com.EDJ.ArCash.Service.interfaces.AccountService;
import com.EDJ.ArCash.Service.interfaces.VirtualCardService;
import com.EDJ.ArCash.Service.impl.AccountServiceImpl;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.observer.Event;
import com.EDJ.ArCash.observer.EventPublisher;
import com.EDJ.ArCash.observer.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountServiceTest {

    private static final long ID_USUARIO = 1L;
    private static final long ID_CUENTA = 10L;

    private static final String ALIAS_GENERADO = "HAPPY.TIGER.AB";
    private static final String CVU_GENERADO = "0000200112345678901234";

    private AccountRepository accountRepository;
    private EventPublisher eventPublisher;
    private VirtualCardService virtualCardService;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        eventPublisher = mock(EventPublisher.class);
        virtualCardService = mock(VirtualCardService.class);

        AccountIdentifierGenerator identifierGenerator = mock(AccountIdentifierGenerator.class);
        when(identifierGenerator.generateUniqueNickname()).thenReturn(ALIAS_GENERADO);
        when(identifierGenerator.generateUniqueCvu()).thenReturn(CVU_GENERADO);

        // El validador es una funcion pura sin dependencias: usarlo de verdad hace
        // que los tests de formato verifiquen la regla real y no la de un mock.
        accountService = new AccountServiceImpl(
                accountRepository,
                eventPublisher,
                identifierGenerator,
                new AliasFormatValidator(),
                virtualCardService);
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
        assertEquals(ALIAS_GENERADO, guardada.getAccountNickname());
        assertEquals(CVU_GENERADO, guardada.getAccountCvu());

        Event evento = capturarEventoPublicado();
        assertEquals(EventType.ACCOUNT_CREATED, evento.getEventType());
        assertEquals(user, evento.getData("user"));
        assertEquals(ALIAS_GENERADO, evento.getData("accountAlias"));
        assertEquals(CVU_GENERADO, evento.getData("accountCvu"));
        verify(virtualCardService).createForAccount(guardada);
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
        verify(virtualCardService).createForAccount(guardada);
    }

    @Test
    @DisplayName("No deja abrir una segunda cuenta en dolares")
    void openUsdAccountRechazaLaSegundaCuenta() {
        User user = usuario();
        when(accountRepository.existsByUserAndAccountType(user, Currency.USD)).thenReturn(true);

        OpenUsdResult result = accountService.openUsdAccount(user);

        assertEquals(OpenUsdResult.Kind.ALREADY_EXISTS, result.getKind());
        assertEquals("El usuario ya cuenta con una cuenta en dolares", result.getMessage());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Abre la cuenta en dolares si el usuario todavia no tiene una")
    void openUsdAccountCreaLaCuentaSiNoExiste() {
        User user = usuario();
        when(accountRepository.existsByUserAndAccountType(user, Currency.USD)).thenReturn(false);

        OpenUsdResult result = accountService.openUsdAccount(user);

        assertEquals(OpenUsdResult.Kind.OK, result.getKind());
        verify(accountRepository).save(any());
    }

    @Test
    @DisplayName("Lista todas las cuentas de un usuario")
    void findAccountsByUserDevuelveLasCuentasDelUsuario() {
        User propietario = usuario();
        Account cuenta = cuenta(propietario);
        when(accountRepository.findAllByUser_Id(ID_USUARIO)).thenReturn(List.of(cuenta));

        assertEquals(List.of(cuenta), accountService.findAccountsByUser(ID_USUARIO));
    }

    // --- updateBalance ---

    @Test
    @DisplayName("El ingreso delega la suma a la base, sin leer y reescribir el saldo")
    void updateBalanceSumaAlSaldoExistente() {
        // Sumar en memoria pierde acreditaciones concurrentes: por eso se espera un UPDATE
        // que arranque del saldo ya comprometido en lugar de un save() de la entidad.
        when(accountRepository.creditBalance(ID_CUENTA, 50.0)).thenReturn(1);

        assertTrue(accountService.updateBalance(50.0, ID_CUENTA));

        verify(accountRepository).creditBalance(ID_CUENTA, 50.0);
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un monto negativo se traduce a un debito con control de saldo")
    void updateBalanceAceptaMontosNegativos() {
        when(accountRepository.debitBalance(ID_CUENTA, 30.0)).thenReturn(1);

        assertTrue(accountService.updateBalance(-30.0, ID_CUENTA));

        // El monto llega en positivo: el signo lo aporta el UPDATE que resta.
        verify(accountRepository).debitBalance(ID_CUENTA, 30.0);
    }

    @Test
    @DisplayName("Un debito sin saldo suficiente no afecta filas y devuelve false")
    void updateBalanceRechazaDebitoSinSaldo() {
        // La condicion de saldo vive en el WHERE del UPDATE: si no alcanza, 0 filas afectadas.
        when(accountRepository.debitBalance(ID_CUENTA, 500.0)).thenReturn(0);

        assertFalse(accountService.updateBalance(-500.0, ID_CUENTA));
    }

    @Test
    @DisplayName("Devuelve false si la cuenta no existe")
    void updateBalanceDevuelveFalseSiLaCuentaNoExiste() {
        when(accountRepository.creditBalance(ID_CUENTA, 50.0)).thenReturn(0);

        assertFalse(accountService.updateBalance(50.0, ID_CUENTA));
        verify(accountRepository, never()).save(any());
    }

    // --- deposit ---

    @Test
    @DisplayName("deposit rechaza monto negativo sin tocar la base")
    void depositRechazaMontoNegativo() {
        DepositResult resultado = accountService.deposit(ID_CUENTA, ID_USUARIO, -1.0);

        assertEquals(DepositResult.Kind.MONTO_NEGATIVO, resultado.getKind());
        assertEquals(-1.0, resultado.getBalance());
        verify(accountRepository, never()).findByIdAccount(any());
    }

    @Test
    @DisplayName("deposit ok: ownership + acreditacion + saldo releido de la base")
    void depositExitosoDevuelveSaldoActualizado() {
        User propietario = usuario();
        Account antes = cuenta(propietario);
        antes.setBalance(500.0);
        Account despues = cuenta(propietario);
        despues.setBalance(600.0);
        // La primera lectura valida la propiedad; la segunda trae el saldo que dejo el UPDATE.
        when(accountRepository.findByIdAccount(ID_CUENTA))
                .thenReturn(Optional.of(antes), Optional.of(despues));
        when(accountRepository.creditBalance(ID_CUENTA, 100.0)).thenReturn(1);

        DepositResult resultado = accountService.deposit(ID_CUENTA, ID_USUARIO, 100.0);

        assertEquals(DepositResult.Kind.OK, resultado.getKind());
        assertEquals(600.0, resultado.getBalance());
    }

    @Test
    @DisplayName("deposit sobre cuenta ajena: NO_ES_PROPIETARIO")
    void depositCuentaAjena() {
        Account ajena = cuenta(usuario());
        ajena.getUser().setId(99L);
        when(accountRepository.findByIdAccount(ID_CUENTA)).thenReturn(Optional.of(ajena));

        DepositResult resultado = accountService.deposit(ID_CUENTA, ID_USUARIO, 100.0);

        assertEquals(DepositResult.Kind.NO_ES_PROPIETARIO, resultado.getKind());
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
