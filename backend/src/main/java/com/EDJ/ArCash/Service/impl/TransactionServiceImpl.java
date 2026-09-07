package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.UsdToArsConversionService;
import com.EDJ.ArCash.Service.interfaces.FavoriteContactService;
import com.EDJ.ArCash.Service.interfaces.ArsToUsdConversionService;
import com.EDJ.ArCash.Service.interfaces.TransactionService;
import com.EDJ.ArCash.Service.result.*;

import com.EDJ.ArCash.DTO.AuthDTO.TransactionDTO;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.FavoriteContact;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.Transaction;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.TransactionRepository;
import com.EDJ.ArCash.observer.Event;
import com.EDJ.ArCash.observer.EventPublisher;
import com.EDJ.ArCash.observer.EventType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final EventPublisher eventPublisher;
    private final ArsToUsdConversionService arsToUsdConversionService;
    private final UsdToArsConversionService usdToArsConversionService;
    private final FavoriteContactService favoriteContactService;


    /**
     * Transferencia con ownership del origen + side-effect lastUsed de favoritos.
     */
    @Transactional
    public OwnedTransferResult transferForOwner(Long userId, Long idOrigen, Long idDestino, double monto) {
        Optional<Account> optionalOrigen = accountRepository.findByIdAccount(idOrigen);
        Optional<Account> optionalDestino = accountRepository.findByIdAccount(idDestino);

        if (optionalOrigen.isEmpty() || optionalDestino.isEmpty()) {
            return OwnedTransferResult.accountNotFound();
        }

        Account origen = optionalOrigen.get();
        if (!origen.getUser().getId().equals(userId)) {
            return OwnedTransferResult.forbidden();
        }

        TransferOperationResult result = transactionWithDetails(idOrigen, idDestino, monto);
        if (result.isSuccess()) {
            touchFavoriteLastUsed(userId, idDestino);
            return OwnedTransferResult.ok();
        }
        return OwnedTransferResult.fail(result.getMessage());
    }

    private void touchFavoriteLastUsed(Long userId, Long destinationAccountId) {
        try {
            List<FavoriteContact> userFavorites = favoriteContactService.getFavoriteContactsByUser(userId);
            Optional<FavoriteContact> matchingFavorite = userFavorites.stream()
                    .filter(favorite -> favorite.getFavoriteAccount().getIdAccount().equals(destinationAccountId))
                    .findFirst();
            matchingFavorite.ifPresent(favorite ->
                    favoriteContactService.updateLastUsedForContact(favorite.getId()));
        } catch (Exception e) {
            System.out.println("Error actualizando lastUsed para contacto favorito: " + e.getMessage());
        }
    }

    @Transactional
    public OwnedBuyUsdResult buyUsdForOwner(Long userId, Long accountArsId, Long accountUsdId, double amountArs) {
        Optional<Account> accountArsOpt = accountRepository.findByIdAccount(accountArsId);
        if (accountArsOpt.isEmpty()) {
            return OwnedBuyUsdResult.arsNotFound();
        }
        if (!accountArsOpt.get().getUser().getId().equals(userId)) {
            return OwnedBuyUsdResult.forbidden();
        }

        BuyUsdResult result = buyUsd(accountArsId, accountUsdId, amountArs);
        return result.isSuccess() ? OwnedBuyUsdResult.ok(result) : OwnedBuyUsdResult.fail(result);
    }

    @Transactional
    public OwnedSellUsdResult sellUsdForOwner(Long userId, Long accountUsdId, Long accountArsId, double amountUsd) {
        Optional<Account> accountUsdOpt = accountRepository.findByIdAccount(accountUsdId);
        if (accountUsdOpt.isEmpty()) {
            return OwnedSellUsdResult.usdNotFound();
        }
        if (!accountUsdOpt.get().getUser().getId().equals(userId)) {
            return OwnedSellUsdResult.forbidden();
        }

        SellUsdResult result = sellUsd(accountUsdId, accountArsId, amountUsd);
        return result.isSuccess() ? OwnedSellUsdResult.ok(result) : OwnedSellUsdResult.fail(result);
    }

    @Transactional
    public boolean transaction(Long idOrigen, Long idDestino, double monto) {
        return transactionWithDetails(idOrigen, idDestino, monto).isSuccess();
    }

    @Transactional
    public TransferOperationResult transactionWithDetails(Long idOrigen, Long idDestino, double monto) {
        if (monto <= 0) {
            return TransferOperationResult.fail("El monto debe ser mayor a cero");
        }

        Optional<Account> optionalOrigen = accountRepository.findByIdAccount(idOrigen);
        Optional<Account> optionalDestino = accountRepository.findByIdAccount(idDestino);

        if (optionalOrigen.isEmpty() || optionalDestino.isEmpty()) {
            return TransferOperationResult.fail("Cuenta no encontrada");
        }

        Account cuentaOrigen = optionalOrigen.get();
        Account cuentaDestino = optionalDestino.get();

        // El filtro JWT solo controla al que hace el pedido. Sin este control, el dinero
        // puede caer en la cuenta de un usuario deshabilitado, que no va a poder retirarlo.
        if (!estaHabilitada(cuentaDestino)) {
            return TransferOperationResult.fail("La cuenta de destino no está habilitada para recibir transferencias");
        }

        if (cuentaOrigen.getAccountType() != cuentaDestino.getAccountType()) {
            return transactionWithConversionDetails(cuentaOrigen, cuentaDestino, monto);
        }

        if (cuentaOrigen.getIdAccount().equals(cuentaDestino.getIdAccount())) {
            transactionSameCurrency(cuentaOrigen, cuentaDestino, monto);
            return TransferOperationResult.fail("No podés transferir a la misma cuenta");
        }

        boolean success = transactionSameCurrency(cuentaOrigen, cuentaDestino, monto);
        if (!success) {
            return TransferOperationResult.fail("Saldo insuficiente o error en la transacción");
        }
        return TransferOperationResult.ok();
    }

    @Transactional
    public boolean transactionSameCurrency(Account cuentaOrigen, Account cuentaDestino, double monto) {
        // Transferencia a uno mismo: marcar como fallida, no mover dinero
        if (cuentaOrigen.getIdAccount().equals(cuentaDestino.getIdAccount())) {
            registrarFallida(cuentaOrigen, cuentaDestino, monto, cuentaOrigen.getAccountType(), null);
            return false;
        }

        Currency moneda = cuentaOrigen.getAccountType();
        String aliasDestino = cuentaDestino.getAccountNickname();
        var titular = cuentaOrigen.getUser();

        if (!debitar(cuentaOrigen.getIdAccount(), monto)) {
            registrarFallida(cuentaOrigen, cuentaDestino, monto, moneda, null);
            return false;
        }
        acreditar(cuentaDestino.getIdAccount(), monto);

        Transaction transaction = nuevoMovimiento(
                cuentaOrigen.getIdAccount(), cuentaDestino.getIdAccount(), monto, moneda);
        transaction.setState("COMPLETED");
        transactionRepository.save(transaction);

        Event event = new Event(EventType.TRANSACTION_COMPLETED);
        event.addData("user", titular);
        event.addData("amount", monto);
        event.addData("destinationAlias", aliasDestino);
        event.addData("currency", moneda.toString());
        event.addData("converted", false);
        event.addData("operationType", "TRANSFER");
        eventPublisher.publish(event);

        return true;
    }

    // =====================================================================
    // Movimiento de saldos
    // =====================================================================

    /**
     * Descuenta un importe del origen solo si el saldo alcanza.
     *
     * <p>El control de saldo lo hace la base dentro del mismo UPDATE. Comprobar antes en Java y
     * descontar despues permitiria que dos transferencias simultaneas pasaran las dos el
     * control y dejaran la cuenta en negativo.
     *
     * @return {@code false} si no habia saldo suficiente (o la cuenta no existe).
     */
    private boolean debitar(Long cuentaId, double monto) {
        return accountRepository.debitBalance(cuentaId, monto) > 0;
    }

    private void acreditar(Long cuentaId, double monto) {
        accountRepository.creditBalance(cuentaId, monto);
    }

    /**
     * Relee una cuenta despues de mover saldos.
     *
     * <p>Los UPDATE atomicos limpian el contexto de persistencia, asi que las instancias
     * cargadas antes quedan con el saldo viejo y desasociadas de la sesion.
     */
    private Account releer(Long cuentaId) {
        return accountRepository.findByIdAccount(cuentaId).orElseThrow();
    }

    /** Movimiento con las cuentas releidas, para no asociar entidades desasociadas. */
    private Transaction nuevoMovimiento(Long origenId, Long destinoId, double monto, Currency moneda) {
        Transaction transaction = new Transaction();
        transaction.setIdOrigin(releer(origenId));
        transaction.setIdDestination(releer(destinoId));
        transaction.setBalance(monto);
        transaction.setCurrency(moneda);
        return transaction;
    }

    /** Una cuenta opera solo si su titular sigue habilitado. */
    private boolean estaHabilitada(Account cuenta) {
        return cuenta.getUser() != null && cuenta.getUser().isActive();
    }

    /** Rechazo por saldo en una conversion ARS→USD: deja el movimiento fallido y el detalle. */
    private TransferOperationResult rechazarPorSaldoArs(
            Account cuentaOrigen,
            Account cuentaDestino,
            double monto,
            DebitPreview preview,
            double saldoOrigen) {
        registrarFallida(cuentaOrigen, cuentaDestino, monto, Currency.ARS, Currency.ARS);
        String message = String.format(
                "Saldo insuficiente. Para enviar $%.2f ARS necesitas $%.2f ARS (incluye $%.2f de comisión). Tu saldo actual es $%.2f ARS",
                monto, preview.totalDebitado(), preview.taxAmount(), saldoOrigen);
        return TransferOperationResult.failInsufficient(
                message, preview.totalDebitado(), saldoOrigen, preview.taxAmount());
    }

    /** Rechazo por saldo en una conversion USD→ARS. */
    private TransferOperationResult rechazarPorSaldoUsd(
            Account cuentaOrigen,
            Account cuentaDestino,
            double monto,
            UsdDebitPreview preview,
            double saldoOrigen) {
        registrarFallida(cuentaOrigen, cuentaDestino, monto, Currency.USD, Currency.USD);
        String message = String.format(
                "Saldo insuficiente. Para enviar U$S%.2f necesitas U$S%.2f (incluye U$S%.2f de comisión). Tu saldo actual es U$S%.2f",
                monto, preview.totalDebitado(), preview.taxAmount(), saldoOrigen);
        return TransferOperationResult.failInsufficient(
                message, preview.totalDebitado(), saldoOrigen, preview.taxAmount());
    }

    /** Deja rastro de un intento rechazado: el historial tiene que mostrar tambien lo que falló. */
    private void registrarFallida(
            Account cuentaOrigen,
            Account cuentaDestino,
            double monto,
            Currency moneda,
            Currency monedaOriginal) {
        Transaction transaction = nuevoMovimiento(
                cuentaOrigen.getIdAccount(), cuentaDestino.getIdAccount(), monto, moneda);
        transaction.setState("FAILED");
        if (monedaOriginal != null) {
            transaction.setOriginalAmount(monto);
            transaction.setOriginalCurrency(monedaOriginal);
        }
        transactionRepository.save(transaction);
    }

  @Transactional
  public TransferOperationResult transactionWithConversionDetails(Account cuentaOrigen, Account cuentaDestino, double monto) {
    boolean esArsAUsd = cuentaOrigen.getAccountType() == Currency.ARS && cuentaDestino.getAccountType() == Currency.USD;
    boolean esUsdAArs = cuentaOrigen.getAccountType() == Currency.USD && cuentaDestino.getAccountType() == Currency.ARS;

    if (!esArsAUsd && !esUsdAArs) {
      registrarFallida(cuentaOrigen, cuentaDestino, monto, cuentaOrigen.getAccountType(), null);
      return TransferOperationResult.fail("Combinación de monedas no soportada");
    }

    if (!cuentaOrigen.getUser().getId().equals(cuentaDestino.getUser().getId())) {
      registrarFallida(cuentaOrigen, cuentaDestino, monto,
        cuentaOrigen.getAccountType(), cuentaOrigen.getAccountType());
      return TransferOperationResult.fail(
        "Para convertir entre ARS y USD, la cuenta destino debe ser tuya. " +
          "Para transferir a otro usuario, elegí una cuenta en la misma moneda que la suya."
      );
    }

    if (esArsAUsd) {
      return convertArsToUsd(cuentaOrigen, cuentaDestino, monto);
    } else {
      return convertUsdToArs(cuentaOrigen, cuentaDestino, monto);
    }
  }

  private TransferOperationResult convertArsToUsd(Account cuentaOrigen, Account cuentaDestino, double monto) {
    DebitPreview preview = arsToUsdConversionService.previewDebit(monto);
    double saldoOrigen = cuentaOrigen.getBalance();
    var titular = cuentaOrigen.getUser();
    String aliasDestino = cuentaDestino.getAccountNickname();

    // Corte temprano con el saldo ya leido: evita pedirle la cotizacion al proveedor externo
    // por una operacion que de todos modos no se puede pagar.
    if (saldoOrigen < preview.totalDebitado()) {
      return rechazarPorSaldoArs(cuentaOrigen, cuentaDestino, monto, preview, saldoOrigen);
    }

    ArsToUsdConversion conversion = arsToUsdConversionService.calculate(monto);

    // El descuento real vuelve a comprobar el saldo dentro del UPDATE: es lo que impide que
    // dos conversiones simultaneas pasen las dos el corte de arriba y dejen la cuenta en rojo.
    if (!debitar(cuentaOrigen.getIdAccount(), conversion.totalDebitado())) {
      return rechazarPorSaldoArs(cuentaOrigen, cuentaDestino, monto, preview, saldoOrigen);
    }
    acreditar(cuentaDestino.getIdAccount(), conversion.amountUsd());

    Transaction transaction = nuevoMovimiento(
      cuentaOrigen.getIdAccount(), cuentaDestino.getIdAccount(), conversion.amountUsd(), Currency.USD);
    transaction.setOriginalAmount(conversion.amountArs());
    transaction.setOriginalCurrency(Currency.ARS);
    transaction.setExchangeRate(conversion.exchangeRate());
    transaction.setTaxAmount(conversion.taxAmount());
    transaction.setTaxPercentage(conversion.taxPercentage());
    transaction.setState("COMPLETED");
    transactionRepository.save(transaction);

    Event event = new Event(EventType.TRANSACTION_COMPLETED);
    event.addData("user", titular);
    event.addData("amount", conversion.amountArs());
    event.addData("amountUsd", conversion.amountUsd());
    event.addData("exchangeRate", conversion.exchangeRate());
    event.addData("taxAmount", conversion.taxAmount());
    event.addData("taxPercentage", conversion.taxPercentage());
    event.addData("totalDebitado", conversion.totalDebitado());
    event.addData("destinationAlias", aliasDestino);
    event.addData("currency", "USD");
    event.addData("converted", true);
    event.addData("operationType", "CONVERSION");
    eventPublisher.publish(event);

    return TransferOperationResult.ok("Transferencia completada exitosamente");
  }

  private TransferOperationResult convertUsdToArs(Account cuentaOrigen, Account cuentaDestino, double monto) {
    UsdDebitPreview preview = usdToArsConversionService.previewDebit(monto);
    double saldoOrigen = cuentaOrigen.getBalance();
    var titular = cuentaOrigen.getUser();
    String aliasDestino = cuentaDestino.getAccountNickname();

    if (saldoOrigen < preview.totalDebitado()) {
      return rechazarPorSaldoUsd(cuentaOrigen, cuentaDestino, monto, preview, saldoOrigen);
    }

    UsdToArsConversion conversion = usdToArsConversionService.calculate(monto);

    if (!debitar(cuentaOrigen.getIdAccount(), conversion.totalDebitado())) {
      return rechazarPorSaldoUsd(cuentaOrigen, cuentaDestino, monto, preview, saldoOrigen);
    }
    acreditar(cuentaDestino.getIdAccount(), conversion.amountArs());

    Transaction transaction = nuevoMovimiento(
      cuentaOrigen.getIdAccount(), cuentaDestino.getIdAccount(), conversion.amountArs(), Currency.ARS);
    transaction.setOriginalAmount(conversion.amountUsd());
    transaction.setOriginalCurrency(Currency.USD);
    transaction.setExchangeRate(conversion.exchangeRate());
    transaction.setTaxAmount(conversion.taxAmount());
    transaction.setTaxPercentage(conversion.taxPercentage());
    transaction.setState("COMPLETED");
    transactionRepository.save(transaction);

    Event event = new Event(EventType.TRANSACTION_COMPLETED);
    event.addData("user", titular);
    event.addData("amount", conversion.amountUsd());
    event.addData("amountArs", conversion.amountArs());
    event.addData("exchangeRate", conversion.exchangeRate());
    event.addData("taxAmount", conversion.taxAmount());
    event.addData("taxPercentage", conversion.taxPercentage());
    event.addData("totalDebitado", conversion.totalDebitado());
    event.addData("destinationAlias", aliasDestino);
    event.addData("currency", "ARS");
    event.addData("converted", true);
    event.addData("operationType", "CONVERSION");
    eventPublisher.publish(event);

    return TransferOperationResult.ok("Transferencia completada exitosamente");
  }



    @Transactional
    public boolean transactionWithConversion(Account cuentaOrigen, Account cuentaDestino, double monto) {
        return transactionWithConversionDetails(cuentaOrigen, cuentaDestino, monto).isSuccess();
    }

    @Transactional
    public BuyUsdResult buyUsd(Long accountArsId, Long accountUsdId, double amountArs) {
        Optional<Account> optionalArs = accountRepository.findByIdAccount(accountArsId);
        Optional<Account> optionalUsd = accountRepository.findByIdAccount(accountUsdId);

        if (optionalArs.isEmpty() || optionalUsd.isEmpty()) {
            return BuyUsdResult.fail("Cuenta no encontrada");
        }

        Account cuentaArs = optionalArs.get();
        Account cuentaUsd = optionalUsd.get();

        if (!cuentaArs.getUser().getId().equals(cuentaUsd.getUser().getId())) {
            return BuyUsdResult.fail("Las cuentas deben pertenecer al mismo usuario");
        }

        if (cuentaArs.getAccountType() != Currency.ARS || cuentaUsd.getAccountType() != Currency.USD) {
            return BuyUsdResult.fail("Debe comprar desde una cuenta en pesos a una cuenta en dólares");
        }

        DebitPreview preview = arsToUsdConversionService.previewDebit(amountArs);
        Long arsId = cuentaArs.getIdAccount();
        Long usdId = cuentaUsd.getIdAccount();
        var titular = cuentaArs.getUser();
        String aliasDestino = cuentaUsd.getAccountNickname();

        String saldoInsuficiente = "Saldo insuficiente. Necesitas $"
                + String.format("%.2f", preview.totalDebitado())
                + " ARS (incluye $" + String.format("%.2f", preview.taxAmount()) + " de comisión)";

        // Corte temprano para no consultar la cotizacion si el saldo ya no alcanza.
        if (cuentaArs.getBalance() < preview.totalDebitado()) {
            return BuyUsdResult.fail(saldoInsuficiente);
        }

        ArsToUsdConversion conversion = arsToUsdConversionService.calculate(amountArs);

        // El UPDATE revalida el saldo: dos compras simultaneas no pueden sobregirar la cuenta.
        if (!debitar(arsId, conversion.totalDebitado())) {
            return BuyUsdResult.fail(saldoInsuficiente);
        }
        acreditar(usdId, conversion.amountUsd());

        Transaction transaction = nuevoMovimiento(arsId, usdId, conversion.amountUsd(), Currency.USD);
        transaction.setOriginalAmount(conversion.amountArs());
        transaction.setOriginalCurrency(Currency.ARS);
        transaction.setExchangeRate(conversion.exchangeRate());
        transaction.setTaxAmount(conversion.taxAmount());
        transaction.setTaxPercentage(conversion.taxPercentage());
        transaction.setState("COMPLETED");
        transactionRepository.save(transaction);

        Event event = new Event(EventType.TRANSACTION_COMPLETED);
        event.addData("user", titular);
        event.addData("amount", conversion.amountArs());
        event.addData("amountUsd", conversion.amountUsd());
        event.addData("exchangeRate", conversion.exchangeRate());
        event.addData("taxAmount", conversion.taxAmount());
        event.addData("taxPercentage", conversion.taxPercentage());
        event.addData("totalDebitado", conversion.totalDebitado());
        event.addData("destinationAlias", aliasDestino);
        event.addData("currency", "USD");
        event.addData("converted", true);
        event.addData("operationType", "BUY_USD");
        eventPublisher.publish(event);

        return BuyUsdResult.ok(
                "Compra de dólares exitosa",
                conversion.amountArs(),
                conversion.amountUsd(),
                conversion.exchangeRate(),
                conversion.taxAmount(),
                conversion.taxPercentage(),
                conversion.totalDebitado(),
                // Saldos releidos de la base: son los que quedaron tras los UPDATE.
                releer(arsId).getBalance(),
                releer(usdId).getBalance()
        );
    }

    @Transactional
    public SellUsdResult sellUsd(Long accountUsdId, Long accountArsId, double amountUsd) {
        Optional<Account> optionalUsd = accountRepository.findByIdAccount(accountUsdId);
        Optional<Account> optionalArs = accountRepository.findByIdAccount(accountArsId);

        if (optionalUsd.isEmpty() || optionalArs.isEmpty()) {
            return SellUsdResult.fail("Cuenta no encontrada");
        }

        Account cuentaUsd = optionalUsd.get();
        Account cuentaArs = optionalArs.get();

        if (!cuentaUsd.getUser().getId().equals(cuentaArs.getUser().getId())) {
            persistSellFailed(cuentaUsd, cuentaArs, amountUsd);
            return SellUsdResult.fail("Las cuentas deben pertenecer al mismo usuario");
        }

        if (cuentaUsd.getAccountType() != Currency.USD || cuentaArs.getAccountType() != Currency.ARS) {
            persistSellFailed(cuentaUsd, cuentaArs, amountUsd);
            return SellUsdResult.fail("Debe vender desde una cuenta en dólares a una cuenta en pesos");
        }

        UsdDebitPreview preview = usdToArsConversionService.previewDebit(amountUsd);
        Long usdId = cuentaUsd.getIdAccount();
        Long arsId = cuentaArs.getIdAccount();
        var titular = cuentaUsd.getUser();
        String aliasDestino = cuentaArs.getAccountNickname();

        String saldoInsuficiente = "Saldo insuficiente. Necesitas $"
                + String.format("%.2f", preview.totalDebitado())
                + " USD (incluye $" + String.format("%.2f", preview.taxAmount()) + " de comisión)";

        if (cuentaUsd.getBalance() < preview.totalDebitado()) {
            persistSellFailed(cuentaUsd, cuentaArs, amountUsd);
            return SellUsdResult.fail(saldoInsuficiente);
        }

        UsdToArsConversion conversion = usdToArsConversionService.calculate(amountUsd);

        if (!debitar(usdId, conversion.totalDebitado())) {
            persistSellFailed(cuentaUsd, cuentaArs, amountUsd);
            return SellUsdResult.fail(saldoInsuficiente);
        }
        acreditar(arsId, conversion.amountArs());

        Transaction transaction = nuevoMovimiento(usdId, arsId, conversion.amountArs(), Currency.ARS);
        transaction.setOriginalAmount(conversion.amountUsd());
        transaction.setOriginalCurrency(Currency.USD);
        transaction.setExchangeRate(conversion.exchangeRate());
        transaction.setTaxAmount(conversion.taxAmount());
        transaction.setTaxPercentage(conversion.taxPercentage());
        transaction.setState("COMPLETED");
        transactionRepository.save(transaction);

        Event event = new Event(EventType.TRANSACTION_COMPLETED);
        event.addData("user", titular);
        event.addData("amount", conversion.amountArs());
        event.addData("amountUsd", conversion.amountUsd());
        event.addData("exchangeRate", conversion.exchangeRate());
        event.addData("taxAmount", conversion.taxAmount());
        event.addData("taxPercentage", conversion.taxPercentage());
        event.addData("totalDebitado", conversion.totalDebitado());
        event.addData("destinationAlias", aliasDestino);
        event.addData("currency", "ARS");
        event.addData("converted", true);
        event.addData("operationType", "SELL_USD");
        eventPublisher.publish(event);

        return SellUsdResult.ok(
                "Venta de dólares exitosa",
                conversion.amountUsd(),
                conversion.amountArs(),
                conversion.exchangeRate(),
                conversion.taxAmount(),
                conversion.taxPercentage(),
                conversion.totalDebitado(),
                releer(arsId).getBalance(),
                releer(usdId).getBalance()
        );
    }

    private void persistSellFailed(Account cuentaUsd, Account cuentaArs, double amountUsd) {
        registrarFallida(cuentaUsd, cuentaArs, amountUsd, Currency.USD, Currency.USD);
    }

    public List<TransactionDTO> listaTransacciones(Long id) {
        Optional<Account> accountOptional = accountRepository.findByIdAccount(id);
        if (accountOptional.isPresent()) {
            List<Transaction> lista = transactionRepository.findByIdOriginOrIdDestination(accountOptional.get(), accountOptional.get());
            lista.sort((t1, t2) -> t2.getTransaction_date().compareTo(t1.getTransaction_date()));
            return lista.stream()
                    .map(TransactionDTO::new)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
