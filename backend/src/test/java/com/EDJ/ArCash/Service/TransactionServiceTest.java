package com.EDJ.ArCash.Service;
import com.EDJ.ArCash.Service.result.BuyUsdResult;
import com.EDJ.ArCash.Service.result.SellUsdResult;
import com.EDJ.ArCash.Service.result.TransferOperationResult;
import com.EDJ.ArCash.Service.interfaces.ArsToUsdConversionService;
import com.EDJ.ArCash.Service.interfaces.CotizationUsdService;
import com.EDJ.ArCash.Service.interfaces.FavoriteContactService;
import com.EDJ.ArCash.Service.interfaces.TaxService;
import com.EDJ.ArCash.Service.interfaces.TransactionService;
import com.EDJ.ArCash.Service.interfaces.UsdToArsConversionService;
import com.EDJ.ArCash.Service.impl.ArsToUsdConversionServiceImpl;
import com.EDJ.ArCash.Service.impl.TaxServiceImpl;
import com.EDJ.ArCash.Service.impl.TransactionServiceImpl;
import com.EDJ.ArCash.Service.impl.UsdToArsConversionServiceImpl;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.Transaction;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.TransactionRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionServiceTest {

    private static final double DELTA = 1e-9;
    private static final long ID_ARS = 10L;
    private static final long ID_USD = 20L;
    private static final long ID_USD_AJENA = 40L;
    private static final long ID_USUARIO = 1L;
    private static final long ID_OTRO = 2L;

    private static final double MONTO_ARS = 10_000.0;
    private static final double TASA_VENTA = 1_000.0;
    private static final double COMISION = 300.0;
    private static final double TOTAL_DEBITO = 10_300.0;
    private static final double USD_ESPERADOS = 10.0;

    private static final double MONTO_USD = 100.0;
    private static final double TASA_COMPRA = 1_000.0;
    private static final double COMISION_USD = 3.0;
    private static final double TOTAL_DEBITO_USD = 103.0;
    private static final double ARS_ESPERADOS = 100_000.0;

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private EventPublisher eventPublisher;
    private CotizationUsdService cotizationUsdService;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        eventPublisher = mock(EventPublisher.class);
        cotizationUsdService = mock(CotizationUsdService.class);
        TaxService taxService = new TaxServiceImpl(cotizationUsdService);
        ArsToUsdConversionService arsToUsdConversionService =
                new ArsToUsdConversionServiceImpl(taxService, cotizationUsdService);
        UsdToArsConversionService usdToArsConversionService =
                new UsdToArsConversionServiceImpl(taxService, cotizationUsdService);

        transactionService = new TransactionServiceImpl(
                accountRepository,
                transactionRepository,
                eventPublisher,
                arsToUsdConversionService,
                usdToArsConversionService,
                mock(FavoriteContactService.class)
        );

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Same-currency ok: debita/acredita, COMPLETED y publica TRANSACTION_COMPLETED")
    void sameCurrencyExitosoPublicaEvento() {
        Account origen = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 500.0);
        Account destino = cuenta(ID_USD, Currency.ARS, ID_OTRO, 100.0);

        boolean ok = transactionService.transactionSameCurrency(origen, destino, 200.0);

        assertTrue(ok);
        assertEquals(300.0, origen.getBalance(), DELTA);
        assertEquals(300.0, destino.getBalance(), DELTA);

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());
        assertEquals("COMPLETED", txnCaptor.getValue().getState());
        assertEquals(200.0, txnCaptor.getValue().getBalance(), DELTA);

        ArgumentCaptor<Event> eventoCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisher).publish(eventoCaptor.capture());
        assertEquals(EventType.TRANSACTION_COMPLETED, eventoCaptor.getValue().getEventType());
        assertEquals(false, eventoCaptor.getValue().getData("converted"));
        assertEquals("TRANSFER", eventoCaptor.getValue().getData("operationType"));
    }

    @Test
    @DisplayName("Same-currency saldo insuficiente: FAILED, no mueve saldos, return false, sin evento")
    void sameCurrencySaldoInsuficiente() {
        Account origen = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 50.0);
        Account destino = cuenta(ID_USD, Currency.ARS, ID_OTRO, 100.0);

        boolean ok = transactionService.transactionSameCurrency(origen, destino, 200.0);

        assertFalse(ok);
        assertEquals(50.0, origen.getBalance(), DELTA);
        assertEquals(100.0, destino.getBalance(), DELTA);

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());
        assertEquals("FAILED", txnCaptor.getValue().getState());
        verify(eventPublisher, never()).publish(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Self-transfer: guarda FAILED, no mueve saldo, return false")
    void selfTransferMarcaFailedYDevuelveFalse() {
        Account misma = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 500.0);

        boolean ok = transactionService.transactionSameCurrency(misma, misma, 100.0);

        assertFalse(ok);
        assertEquals(500.0, misma.getBalance(), DELTA);

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());
        assertEquals("FAILED", txnCaptor.getValue().getState());
        verify(accountRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Self-transfer via orquestador: mensaje especifico, no el de saldo insuficiente")
    void selfTransferViaOrquestadorDevuelveMensajeEspecifico() {
        Account misma = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 500.0);
        when(accountRepository.findByIdAccount(ID_ARS)).thenReturn(Optional.of(misma));

        TransferOperationResult result =
                transactionService.transactionWithDetails(ID_ARS, ID_ARS, 100.0);

        assertFalse(result.isSuccess());
        assertEquals("No podés transferir a la misma cuenta", result.getMessage());
        assertEquals(500.0, misma.getBalance(), DELTA);

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());
        assertEquals("FAILED", txnCaptor.getValue().getState());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Conversion ARS→USD mismo usuario: 3%, usd=ars/venta, comision NO se convierte a USD, evento si")
    void conversionArsAUsdMismaUsuarioFormulaYComisionNoConvertida() {
        Account ars = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 20_000.0);
        Account usd = cuenta(ID_USD, Currency.USD, ID_USUARIO, 5.0);
        when(cotizationUsdService.obtenerCotizacionVenta()).thenReturn(TASA_VENTA);

        TransferOperationResult result =
                transactionService.transactionWithConversionDetails(ars, usd, MONTO_ARS);

        assertTrue(result.isSuccess());
        assertEquals(20_000.0 - TOTAL_DEBITO, ars.getBalance(), DELTA);

        double usdAcreditados = usd.getBalance() - 5.0;
        assertEquals(10.0, usdAcreditados, DELTA);
        assertEquals(10.3, TOTAL_DEBITO / TASA_VENTA, DELTA);
        assertEquals(COMISION / TASA_VENTA, 0.3, DELTA);
        assertEquals(usdAcreditados + (COMISION / TASA_VENTA), 10.3, DELTA);

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());
        Transaction txn = txnCaptor.getValue();
        assertEquals("COMPLETED", txn.getState());
        assertEquals(10.0, txn.getBalance(), DELTA);
        assertEquals(MONTO_ARS, txn.getOriginalAmount(), DELTA);
        assertEquals(COMISION, txn.getTaxAmount(), DELTA);
        assertEquals(3.0, txn.getTaxPercentage(), DELTA);
        assertEquals(TASA_VENTA, txn.getExchangeRate(), DELTA);

        ArgumentCaptor<Event> eventoCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisher).publish(eventoCaptor.capture());
        assertEquals("CONVERSION", eventoCaptor.getValue().getData("operationType"));
    }

    @Test
    @DisplayName("Conversion saldo insuficiente: FAILED, montos en result, y NO consulta cotizacion")
    void conversionSaldoInsuficiente() {
        Account ars = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 100.0);
        Account usd = cuenta(ID_USD, Currency.USD, ID_USUARIO, 0.0);

        TransferOperationResult result =
                transactionService.transactionWithConversionDetails(ars, usd, MONTO_ARS);

        assertFalse(result.isSuccess());
        assertEquals(TOTAL_DEBITO, result.getMontoRequerido(), DELTA);
        assertEquals(100.0, result.getSaldoActual(), DELTA);
        assertEquals(COMISION, result.getImpuestos(), DELTA);
        assertEquals(100.0, ars.getBalance(), DELTA);

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());
        assertEquals("FAILED", txnCaptor.getValue().getState());
        verify(eventPublisher, never()).publish(any());
        verify(cotizationUsdService, never()).obtenerCotizacionVenta();
    }

    @Test
    @DisplayName("Conversion USD→ARS: FAILED y mensaje Solo se permite conversion de ARS a USD")
    void conversionDireccionInvalida() {
        Account usd = cuenta(ID_USD, Currency.USD, ID_USUARIO, 50.0);
        Account ars = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 1_000.0);

        TransferOperationResult result =
                transactionService.transactionWithConversionDetails(usd, ars, 10.0);

        assertFalse(result.isSuccess());
        assertEquals("Solo se permite conversión de ARS a USD", result.getMessage());

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());
        assertEquals("FAILED", txnCaptor.getValue().getState());
        verify(eventPublisher, never()).publish(any());
        verify(cotizationUsdService, never()).obtenerCotizacionVenta();
    }

    @Test
    @DisplayName("Conversion ARS→USD a cuenta ajena: FAILED, sin mover saldos, sin evento")
    void conversionACuentaAjenaFallaConMismoDuenoRequerido() {
        Account arsPropia = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 20_000.0);
        Account usdAjena = cuenta(ID_USD_AJENA, Currency.USD, ID_OTRO, 1.0);

        TransferOperationResult result =
                transactionService.transactionWithConversionDetails(arsPropia, usdAjena, MONTO_ARS);

        assertFalse(result.isSuccess());
        assertEquals("Las cuentas deben pertenecer al mismo usuario", result.getMessage());
        assertEquals(20_000.0, arsPropia.getBalance(), DELTA);
        assertEquals(1.0, usdAjena.getBalance(), DELTA);

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());
        assertEquals("FAILED", txnCaptor.getValue().getState());
        verify(accountRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
        verify(cotizationUsdService, never()).obtenerCotizacionVenta();
    }

    @Test
    @DisplayName("buyUsd ok: misma formula 3%+venta, result rico, publica evento como conversion")
    void buyUsdExitosoPublicaEvento() {
        Account ars = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 20_000.0);
        Account usd = cuenta(ID_USD, Currency.USD, ID_USUARIO, 5.0);
        when(accountRepository.findByIdAccount(ID_ARS)).thenReturn(Optional.of(ars));
        when(accountRepository.findByIdAccount(ID_USD)).thenReturn(Optional.of(usd));
        when(cotizationUsdService.obtenerCotizacionVenta()).thenReturn(TASA_VENTA);

        BuyUsdResult result = transactionService.buyUsd(ID_ARS, ID_USD, MONTO_ARS);

        assertTrue(result.isSuccess());
        assertEquals(MONTO_ARS, result.getAmountArs(), DELTA);
        assertEquals(USD_ESPERADOS, result.getAmountUsd(), DELTA);
        assertEquals(TASA_VENTA, result.getExchangeRate(), DELTA);
        assertEquals(COMISION, result.getTaxAmount(), DELTA);
        assertEquals(3.0, result.getTaxPercentage(), DELTA);
        assertEquals(TOTAL_DEBITO, result.getTotalDebitado(), DELTA);
        assertEquals(20_000.0 - TOTAL_DEBITO, result.getNewBalanceArs(), DELTA);
        assertEquals(5.0 + USD_ESPERADOS, result.getNewBalanceUsd(), DELTA);
        assertEquals(USD_ESPERADOS, result.getAmountUsd(), DELTA);
        assertTrue(USD_ESPERADOS < TOTAL_DEBITO / TASA_VENTA);

        ArgumentCaptor<Event> eventoCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisher).publish(eventoCaptor.capture());
        Event evento = eventoCaptor.getValue();
        assertEquals(EventType.TRANSACTION_COMPLETED, evento.getEventType());
        assertEquals(true, evento.getData("converted"));
        assertEquals(MONTO_ARS, (Double) evento.getData("amount"), DELTA);
        assertEquals(USD_ESPERADOS, (Double) evento.getData("amountUsd"), DELTA);
        assertEquals(TASA_VENTA, (Double) evento.getData("exchangeRate"), DELTA);
        assertEquals(COMISION, (Double) evento.getData("taxAmount"), DELTA);
        assertEquals(3.0, (Double) evento.getData("taxPercentage"), DELTA);
        assertEquals(TOTAL_DEBITO, (Double) evento.getData("totalDebitado"), DELTA);
        assertEquals("ALIAS." + ID_USD, evento.getData("destinationAlias"));
        assertEquals("USD", evento.getData("currency"));
        assertEquals("BUY_USD", evento.getData("operationType"));
        assertEquals(ars.getUser(), evento.getData("user"));
    }

    @Test
    @DisplayName("buyUsd con cuentas de distinto usuario: success=false sin persistir")
    void buyUsdCuentasDeDistintoUsuario() {
        Account ars = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 20_000.0);
        Account usdAjena = cuenta(ID_USD_AJENA, Currency.USD, ID_OTRO, 0.0);
        when(accountRepository.findByIdAccount(ID_ARS)).thenReturn(Optional.of(ars));
        when(accountRepository.findByIdAccount(ID_USD_AJENA)).thenReturn(Optional.of(usdAjena));

        BuyUsdResult result = transactionService.buyUsd(ID_ARS, ID_USD_AJENA, MONTO_ARS);

        assertFalse(result.isSuccess());
        assertEquals("Las cuentas deben pertenecer al mismo usuario", result.getMessage());
        assertNull(result.getAmountUsd());
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("buyUsd saldo insuficiente: fail sin fila FAILED, sin cotizacion")
    void buyUsdSaldoInsuficienteSinFilaFailed() {
        Account ars = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 100.0);
        Account usd = cuenta(ID_USD, Currency.USD, ID_USUARIO, 0.0);
        when(accountRepository.findByIdAccount(ID_ARS)).thenReturn(Optional.of(ars));
        when(accountRepository.findByIdAccount(ID_USD)).thenReturn(Optional.of(usd));

        BuyUsdResult result = transactionService.buyUsd(ID_ARS, ID_USD, MONTO_ARS);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Saldo insuficiente"));
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
        verify(cotizationUsdService, never()).obtenerCotizacionVenta();
    }

    @Test
    @DisplayName("S1 sellUsd ok: 3%+compra, result rico, COMPLETED, publica evento, usa compra no venta")
    void sellUsdExitosoPublicaEvento() {
        Account usd = cuenta(ID_USD, Currency.USD, ID_USUARIO, 200.0);
        Account ars = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 1_000.0);
        when(accountRepository.findByIdAccount(ID_USD)).thenReturn(Optional.of(usd));
        when(accountRepository.findByIdAccount(ID_ARS)).thenReturn(Optional.of(ars));
        when(cotizationUsdService.obtenerCotizacionCompra()).thenReturn(TASA_COMPRA);

        SellUsdResult result = transactionService.sellUsd(ID_USD, ID_ARS, MONTO_USD);

        assertTrue(result.isSuccess());
        assertEquals(MONTO_USD, result.getAmountUsd(), DELTA);
        assertEquals(ARS_ESPERADOS, result.getAmountArs(), DELTA);
        assertEquals(TASA_COMPRA, result.getExchangeRate(), DELTA);
        assertEquals(COMISION_USD, result.getTaxAmount(), DELTA);
        assertEquals(3.0, result.getTaxPercentage(), DELTA);
        assertEquals(TOTAL_DEBITO_USD, result.getTotalDebitado(), DELTA);
        assertEquals(200.0 - TOTAL_DEBITO_USD, result.getNewBalanceUsd(), DELTA);
        assertEquals(1_000.0 + ARS_ESPERADOS, result.getNewBalanceArs(), DELTA);
        assertEquals(200.0 - TOTAL_DEBITO_USD, usd.getBalance(), DELTA);
        assertEquals(1_000.0 + ARS_ESPERADOS, ars.getBalance(), DELTA);

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());
        assertEquals("COMPLETED", txnCaptor.getValue().getState());
        assertEquals(ARS_ESPERADOS, txnCaptor.getValue().getBalance(), DELTA);

        ArgumentCaptor<Event> eventoCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventPublisher).publish(eventoCaptor.capture());
        Event evento = eventoCaptor.getValue();
        assertEquals(EventType.TRANSACTION_COMPLETED, evento.getEventType());
        assertEquals(true, evento.getData("converted"));
        assertEquals(ARS_ESPERADOS, (Double) evento.getData("amount"), DELTA);
        assertEquals(MONTO_USD, (Double) evento.getData("amountUsd"), DELTA);
        assertEquals(TASA_COMPRA, (Double) evento.getData("exchangeRate"), DELTA);
        assertEquals("ARS", evento.getData("currency"));
        assertEquals("ALIAS." + ID_ARS, evento.getData("destinationAlias"));
        assertEquals("SELL_USD", evento.getData("operationType"));

        verify(cotizationUsdService).obtenerCotizacionCompra();
        verify(cotizationUsdService, never()).obtenerCotizacionVenta();
    }

    @Test
    @DisplayName("S2 sellUsd ownership: FAILED, mensaje 7.3.2, sin saldos ni cotizacion")
    void sellUsdCuentasDeDistintoUsuario() {
        Account usd = cuenta(ID_USD, Currency.USD, ID_USUARIO, 200.0);
        Account arsAjena = cuenta(ID_ARS, Currency.ARS, ID_OTRO, 1_000.0);
        when(accountRepository.findByIdAccount(ID_USD)).thenReturn(Optional.of(usd));
        when(accountRepository.findByIdAccount(ID_ARS)).thenReturn(Optional.of(arsAjena));

        SellUsdResult result = transactionService.sellUsd(ID_USD, ID_ARS, MONTO_USD);

        assertFalse(result.isSuccess());
        assertEquals("Las cuentas deben pertenecer al mismo usuario", result.getMessage());
        assertEquals(200.0, usd.getBalance(), DELTA);
        assertEquals(1_000.0, arsAjena.getBalance(), DELTA);

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());
        assertEquals("FAILED", txnCaptor.getValue().getState());
        verify(accountRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
        verify(cotizationUsdService, never()).obtenerCotizacionCompra();
        verify(cotizationUsdService, never()).obtenerCotizacionVenta();
    }

    @Test
    @DisplayName("S3 sellUsd saldo insuficiente: FAILED y NO consulta cotizacion")
    void sellUsdSaldoInsuficienteSinCotizacion() {
        Account usd = cuenta(ID_USD, Currency.USD, ID_USUARIO, 50.0);
        Account ars = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 1_000.0);
        when(accountRepository.findByIdAccount(ID_USD)).thenReturn(Optional.of(usd));
        when(accountRepository.findByIdAccount(ID_ARS)).thenReturn(Optional.of(ars));

        SellUsdResult result = transactionService.sellUsd(ID_USD, ID_ARS, MONTO_USD);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Saldo insuficiente"));
        assertEquals(50.0, usd.getBalance(), DELTA);

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());
        assertEquals("FAILED", txnCaptor.getValue().getState());
        verify(eventPublisher, never()).publish(any());
        verify(cotizationUsdService, never()).obtenerCotizacionCompra();
        verify(cotizationUsdService, never()).obtenerCotizacionVenta();
    }

    @Test
    @DisplayName("S4 sellUsd cuenta no encontrada o tipo invalido: fail; tipo invalido persiste FAILED")
    void sellUsdCuentaNoEncontradaOTipoInvalido() {
        when(accountRepository.findByIdAccount(ID_USD)).thenReturn(Optional.empty());
        when(accountRepository.findByIdAccount(ID_ARS)).thenReturn(Optional.empty());

        SellUsdResult noEncontrada = transactionService.sellUsd(ID_USD, ID_ARS, MONTO_USD);

        assertFalse(noEncontrada.isSuccess());
        assertEquals("Cuenta no encontrada", noEncontrada.getMessage());
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());

        Account origenArs = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 20_000.0);
        Account destinoUsd = cuenta(ID_USD, Currency.USD, ID_USUARIO, 50.0);
        when(accountRepository.findByIdAccount(ID_ARS)).thenReturn(Optional.of(origenArs));
        when(accountRepository.findByIdAccount(ID_USD)).thenReturn(Optional.of(destinoUsd));

        SellUsdResult tipoInvalido = transactionService.sellUsd(ID_ARS, ID_USD, MONTO_USD);

        assertFalse(tipoInvalido.isSuccess());
        assertEquals("Debe vender desde una cuenta en dólares a una cuenta en pesos", tipoInvalido.getMessage());

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());
        assertEquals("FAILED", txnCaptor.getValue().getState());
        verify(eventPublisher, never()).publish(any());
        verify(cotizationUsdService, never()).obtenerCotizacionCompra();
    }

    private Account cuenta(long id, Currency tipo, long userId, double balance) {
        User user = new User();
        user.setId(userId);
        Account account = new Account();
        account.setIdAccount(id);
        account.setUser(user);
        account.setAccountType(tipo);
        account.setBalance(balance);
        account.setAccountNickname("ALIAS." + id);
        return account;
    }
}
