package com.EDJ.ArCash.Service;

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

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Caracterizacion del comportamiento ACTUAL de TransactionService (saldos, conversion, eventos).
 * Red de seguridad antes de refactor 7.1+ y fixes 7.3.
 */
class TransactionServiceTest {

    private static final double DELTA = 1e-9;
    private static final long ID_ARS = 10L;
    private static final long ID_USD = 20L;
    private static final long ID_USD_AJENA = 40L;
    private static final long ID_USUARIO = 1L;
    private static final long ID_OTRO = 2L;

    /** Numeros redondos para clavar la formula: 10000 ARS, venta 1000, comision 3%. */
    private static final double MONTO_ARS = 10_000.0;
    private static final double TASA_VENTA = 1_000.0;
    private static final double COMISION = 300.0;          // 3% de 10000
    private static final double TOTAL_DEBITO = 10_300.0;   // base + comision
    private static final double USD_ESPERADOS = 10.0;      // 10000 / 1000 — SIN convertir la comision

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
        TaxService taxService = new TaxService(cotizationUsdService);
        ArsToUsdConversionService conversionService =
                new ArsToUsdConversionService(taxService, cotizationUsdService);

        transactionService = new TransactionService(
                accountRepository,
                transactionRepository,
                eventPublisher,
                conversionService
        );

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- B1 ---

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
    }

    // --- B2 ---

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

    // --- B3 ---

    @Test
    @DisplayName("Self-transfer: guarda FAILED, no mueve saldo, pero return true (bug documentado)")
    void selfTransferMarcaFailedPeroDevuelveTrue() {
        Account misma = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 500.0);

        boolean ok = transactionService.transactionSameCurrency(misma, misma, 100.0);

        assertTrue(ok);
        assertEquals(500.0, misma.getBalance(), DELTA);

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());
        assertEquals("FAILED", txnCaptor.getValue().getState());
        verify(accountRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    // --- B4 ---

    @Test
    @DisplayName("Conversion ARS→USD mismo usuario: 3%, usd=ars/venta, comision NO se convierte a USD, evento si")
    void conversionArsAUsdMismaUsuarioFormulaYComisionNoConvertida() {
        Account ars = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 20_000.0);
        Account usd = cuenta(ID_USD, Currency.USD, ID_USUARIO, 5.0);
        when(cotizationUsdService.obtenerCotizacionVenta()).thenReturn(TASA_VENTA);

        Map<String, Object> result = transactionService.transactionWithConversionDetails(ars, usd, MONTO_ARS);

        assertTrue((Boolean) result.get("success"));
        assertEquals(20_000.0 - TOTAL_DEBITO, ars.getBalance(), DELTA);

        // Numeros concretos: se debitan 10300 ARS (base+3%) pero solo se acreditan
        // 10.00 USD = 10000/1000. Si la comision se convirtiera, serian 10.30 USD.
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

        verify(eventPublisher).publish(any(Event.class));
    }

    // --- B5 ---

    @Test
    @DisplayName("Conversion saldo insuficiente: FAILED, map con montos, y NO consulta cotizacion")
    void conversionSaldoInsuficiente() {
        Account ars = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 100.0);
        Account usd = cuenta(ID_USD, Currency.USD, ID_USUARIO, 0.0);

        Map<String, Object> result = transactionService.transactionWithConversionDetails(ars, usd, MONTO_ARS);

        assertFalse((Boolean) result.get("success"));
        assertEquals(TOTAL_DEBITO, (Double) result.get("montoRequerido"), DELTA);
        assertEquals(100.0, (Double) result.get("saldoActual"), DELTA);
        assertEquals(COMISION, (Double) result.get("impuestos"), DELTA);
        assertEquals(100.0, ars.getBalance(), DELTA);

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());
        assertEquals("FAILED", txnCaptor.getValue().getState());
        verify(eventPublisher, never()).publish(any());
        verify(cotizationUsdService, never()).obtenerCotizacionVenta();
    }

    // --- B6 ---

    @Test
    @DisplayName("Conversion USD→ARS: FAILED y mensaje Solo se permite conversion de ARS a USD")
    void conversionDireccionInvalida() {
        Account usd = cuenta(ID_USD, Currency.USD, ID_USUARIO, 50.0);
        Account ars = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 1_000.0);

        Map<String, Object> result = transactionService.transactionWithConversionDetails(usd, ars, 10.0);

        assertFalse((Boolean) result.get("success"));
        assertEquals("Solo se permite conversión de ARS a USD", result.get("message"));

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());
        assertEquals("FAILED", txnCaptor.getValue().getState());
        verify(eventPublisher, never()).publish(any());
        verify(cotizationUsdService, never()).obtenerCotizacionVenta();
    }

    // --- B7 ---

    @Test
    @DisplayName("BUG ownership: conversion ARS→USD a cuenta de otro usuario hoy tiene success=true")
    void conversionACuentaAjenaHoyEstaPermitida() {
        Account arsPropia = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 20_000.0);
        Account usdAjena = cuenta(ID_USD_AJENA, Currency.USD, ID_OTRO, 1.0);
        when(cotizationUsdService.obtenerCotizacionVenta()).thenReturn(TASA_VENTA);

        Map<String, Object> result =
                transactionService.transactionWithConversionDetails(arsPropia, usdAjena, MONTO_ARS);

        assertTrue((Boolean) result.get("success"));
        assertEquals(20_000.0 - TOTAL_DEBITO, arsPropia.getBalance(), DELTA);
        assertEquals(1.0 + USD_ESPERADOS, usdAjena.getBalance(), DELTA);
        verify(eventPublisher).publish(any(Event.class));
    }

    // --- B8 ---

    @Test
    @DisplayName("buyUsd ok: misma formula 3%+venta, map rico, NO publica evento")
    void buyUsdExitosoSinEvento() {
        Account ars = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 20_000.0);
        Account usd = cuenta(ID_USD, Currency.USD, ID_USUARIO, 5.0);
        when(accountRepository.findByIdAccount(ID_ARS)).thenReturn(Optional.of(ars));
        when(accountRepository.findByIdAccount(ID_USD)).thenReturn(Optional.of(usd));
        when(cotizationUsdService.obtenerCotizacionVenta()).thenReturn(TASA_VENTA);

        Map<String, Object> result = transactionService.buyUsd(ID_ARS, ID_USD, MONTO_ARS);

        assertTrue((Boolean) result.get("success"));
        assertEquals(MONTO_ARS, (Double) result.get("amountArs"), DELTA);
        assertEquals(USD_ESPERADOS, (Double) result.get("amountUsd"), DELTA);
        assertEquals(TASA_VENTA, (Double) result.get("exchangeRate"), DELTA);
        assertEquals(COMISION, (Double) result.get("taxAmount"), DELTA);
        assertEquals(3.0, (Double) result.get("taxPercentage"), DELTA);
        assertEquals(TOTAL_DEBITO, (Double) result.get("totalDebitado"), DELTA);
        assertEquals(20_000.0 - TOTAL_DEBITO, (Double) result.get("newBalanceArs"), DELTA);
        assertEquals(5.0 + USD_ESPERADOS, (Double) result.get("newBalanceUsd"), DELTA);
        // Comision no convertida (mismo numero concreto que B4)
        assertEquals(USD_ESPERADOS, (Double) result.get("amountUsd"), DELTA);
        assertTrue(USD_ESPERADOS < TOTAL_DEBITO / TASA_VENTA);

        verify(eventPublisher, never()).publish(any());
    }

    // --- B9 ---

    @Test
    @DisplayName("buyUsd con cuentas de distinto usuario: success=false sin persistir")
    void buyUsdCuentasDeDistintoUsuario() {
        Account ars = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 20_000.0);
        Account usdAjena = cuenta(ID_USD_AJENA, Currency.USD, ID_OTRO, 0.0);
        when(accountRepository.findByIdAccount(ID_ARS)).thenReturn(Optional.of(ars));
        when(accountRepository.findByIdAccount(ID_USD_AJENA)).thenReturn(Optional.of(usdAjena));

        Map<String, Object> result = transactionService.buyUsd(ID_ARS, ID_USD_AJENA, MONTO_ARS);

        assertFalse((Boolean) result.get("success"));
        assertEquals("Las cuentas deben pertenecer al mismo usuario", result.get("message"));
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    // --- B10 ---

    @Test
    @DisplayName("buyUsd saldo insuficiente: fail solo con map, sin fila FAILED, sin cotizacion")
    void buyUsdSaldoInsuficienteSinFilaFailed() {
        Account ars = cuenta(ID_ARS, Currency.ARS, ID_USUARIO, 100.0);
        Account usd = cuenta(ID_USD, Currency.USD, ID_USUARIO, 0.0);
        when(accountRepository.findByIdAccount(ID_ARS)).thenReturn(Optional.of(ars));
        when(accountRepository.findByIdAccount(ID_USD)).thenReturn(Optional.of(usd));

        Map<String, Object> result = transactionService.buyUsd(ID_ARS, ID_USD, MONTO_ARS);

        assertFalse((Boolean) result.get("success"));
        assertTrue(((String) result.get("message")).contains("Saldo insuficiente"));
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
        verify(cotizationUsdService, never()).obtenerCotizacionVenta();
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
