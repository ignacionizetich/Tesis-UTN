package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.DTO.AuthDTO.TransactionDTO;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.Transaction;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.TransactionRepository;
import com.EDJ.ArCash.observer.Event;
import com.EDJ.ArCash.observer.EventPublisher;
import com.EDJ.ArCash.observer.EventType;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final EventPublisher eventPublisher;
    private final ArsToUsdConversionService arsToUsdConversionService;
    private final UsdToArsConversionService usdToArsConversionService;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              EventPublisher eventPublisher,
                              ArsToUsdConversionService arsToUsdConversionService,
                              UsdToArsConversionService usdToArsConversionService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
        this.arsToUsdConversionService = arsToUsdConversionService;
        this.usdToArsConversionService = usdToArsConversionService;
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
        Transaction transaction = new Transaction();

        // Transferencia a uno mismo: marcar como fallida, no mover dinero
        if (cuentaOrigen.getIdAccount().equals(cuentaDestino.getIdAccount())) {
            transaction.setIdOrigin(cuentaOrigen);
            transaction.setIdDestination(cuentaDestino);
            transaction.setBalance(monto);
            transaction.setState("FAILED");
            transaction.setCurrency(cuentaOrigen.getAccountType());
            transactionRepository.save(transaction);
            return false;
        }

        if (cuentaOrigen.getBalance() < monto) {
            transaction.setIdOrigin(cuentaOrigen);
            transaction.setIdDestination(cuentaDestino);
            transaction.setBalance(monto);
            transaction.setState("FAILED");
            transaction.setCurrency(cuentaOrigen.getAccountType());
            transactionRepository.save(transaction);
            return false;
        } else {
            cuentaOrigen.setBalance(cuentaOrigen.getBalance() - monto);
            cuentaDestino.setBalance(cuentaDestino.getBalance() + monto);
            transaction.setIdOrigin(cuentaOrigen);
            transaction.setIdDestination(cuentaDestino);
            transaction.setBalance(monto);
            transaction.setState("COMPLETED");
            transaction.setCurrency(cuentaOrigen.getAccountType());
            accountRepository.save(cuentaOrigen);
            accountRepository.save(cuentaDestino);
            transactionRepository.save(transaction);

            Event event = new Event(EventType.TRANSACTION_COMPLETED);
            event.addData("user", cuentaOrigen.getUser());
            event.addData("amount", monto);
            event.addData("destinationAlias", cuentaDestino.getAccountNickname());
            event.addData("currency", cuentaOrigen.getAccountType().toString());
            event.addData("converted", false);
            event.addData("operationType", "TRANSFER");
            eventPublisher.publish(event);

            return true;
        }
    }

    @Transactional
    public TransferOperationResult transactionWithConversionDetails(Account cuentaOrigen, Account cuentaDestino, double monto) {
        Transaction transaction = new Transaction();

        if (cuentaOrigen.getAccountType() != Currency.ARS || cuentaDestino.getAccountType() != Currency.USD) {
            transaction.setIdOrigin(cuentaOrigen);
            transaction.setIdDestination(cuentaDestino);
            transaction.setBalance(monto);
            transaction.setState("FAILED");
            transaction.setCurrency(cuentaOrigen.getAccountType());
            transactionRepository.save(transaction);
            return TransferOperationResult.fail("Solo se permite conversión de ARS a USD");
        }

        if (!cuentaOrigen.getUser().getId().equals(cuentaDestino.getUser().getId())) {
            transaction.setIdOrigin(cuentaOrigen);
            transaction.setIdDestination(cuentaDestino);
            transaction.setBalance(monto);
            transaction.setState("FAILED");
            transaction.setCurrency(Currency.ARS);
            transaction.setOriginalAmount(monto);
            transaction.setOriginalCurrency(Currency.ARS);
            transactionRepository.save(transaction);
            return TransferOperationResult.fail("Las cuentas deben pertenecer al mismo usuario");
        }

        DebitPreview preview = arsToUsdConversionService.previewDebit(monto);

        if (cuentaOrigen.getBalance() < preview.totalDebitado()) {
            transaction.setIdOrigin(cuentaOrigen);
            transaction.setIdDestination(cuentaDestino);
            transaction.setBalance(monto);
            transaction.setState("FAILED");
            transaction.setCurrency(Currency.ARS);
            transaction.setOriginalAmount(monto);
            transaction.setOriginalCurrency(Currency.ARS);
            transactionRepository.save(transaction);

            String message = String.format(
                    "Saldo insuficiente. Para enviar $%.2f ARS necesitas $%.2f ARS (incluye $%.2f de comisión). Tu saldo actual es $%.2f ARS",
                    monto, preview.totalDebitado(), preview.taxAmount(), cuentaOrigen.getBalance());
            return TransferOperationResult.failInsufficient(
                    message, preview.totalDebitado(), cuentaOrigen.getBalance(), preview.taxAmount());
        }

        ArsToUsdConversion conversion = arsToUsdConversionService.calculate(monto);

        cuentaOrigen.setBalance(cuentaOrigen.getBalance() - conversion.totalDebitado());
        cuentaDestino.setBalance(cuentaDestino.getBalance() + conversion.amountUsd());

        transaction.setIdOrigin(cuentaOrigen);
        transaction.setIdDestination(cuentaDestino);
        transaction.setBalance(conversion.amountUsd());
        transaction.setOriginalAmount(conversion.amountArs());
        transaction.setOriginalCurrency(Currency.ARS);
        transaction.setCurrency(Currency.USD);
        transaction.setExchangeRate(conversion.exchangeRate());
        transaction.setTaxAmount(conversion.taxAmount());
        transaction.setTaxPercentage(conversion.taxPercentage());
        transaction.setState("COMPLETED");

        accountRepository.save(cuentaOrigen);
        accountRepository.save(cuentaDestino);
        transactionRepository.save(transaction);

        Event event = new Event(EventType.TRANSACTION_COMPLETED);
        event.addData("user", cuentaOrigen.getUser());
        event.addData("amount", conversion.amountArs());
        event.addData("amountUsd", conversion.amountUsd());
        event.addData("exchangeRate", conversion.exchangeRate());
        event.addData("taxAmount", conversion.taxAmount());
        event.addData("taxPercentage", conversion.taxPercentage());
        event.addData("totalDebitado", conversion.totalDebitado());
        event.addData("destinationAlias", cuentaDestino.getAccountNickname());
        event.addData("currency", "USD");
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

        if (cuentaArs.getBalance() < preview.totalDebitado()) {
            return BuyUsdResult.fail("Saldo insuficiente. Necesitas $" + String.format("%.2f", preview.totalDebitado()) +
                    " ARS (incluye $" + String.format("%.2f", preview.taxAmount()) + " de comisión)");
        }

        ArsToUsdConversion conversion = arsToUsdConversionService.calculate(amountArs);

        cuentaArs.setBalance(cuentaArs.getBalance() - conversion.totalDebitado());
        cuentaUsd.setBalance(cuentaUsd.getBalance() + conversion.amountUsd());

        Transaction transaction = new Transaction();
        transaction.setIdOrigin(cuentaArs);
        transaction.setIdDestination(cuentaUsd);
        transaction.setBalance(conversion.amountUsd());
        transaction.setOriginalAmount(conversion.amountArs());
        transaction.setOriginalCurrency(Currency.ARS);
        transaction.setCurrency(Currency.USD);
        transaction.setExchangeRate(conversion.exchangeRate());
        transaction.setTaxAmount(conversion.taxAmount());
        transaction.setTaxPercentage(conversion.taxPercentage());
        transaction.setState("COMPLETED");

        accountRepository.save(cuentaArs);
        accountRepository.save(cuentaUsd);
        transactionRepository.save(transaction);

        Event event = new Event(EventType.TRANSACTION_COMPLETED);
        event.addData("user", cuentaArs.getUser());
        event.addData("amount", conversion.amountArs());
        event.addData("amountUsd", conversion.amountUsd());
        event.addData("exchangeRate", conversion.exchangeRate());
        event.addData("taxAmount", conversion.taxAmount());
        event.addData("taxPercentage", conversion.taxPercentage());
        event.addData("totalDebitado", conversion.totalDebitado());
        event.addData("destinationAlias", cuentaUsd.getAccountNickname());
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
                cuentaArs.getBalance(),
                cuentaUsd.getBalance()
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

        if (cuentaUsd.getBalance() < preview.totalDebitado()) {
            persistSellFailed(cuentaUsd, cuentaArs, amountUsd);
            return SellUsdResult.fail("Saldo insuficiente. Necesitas $" + String.format("%.2f", preview.totalDebitado()) +
                    " USD (incluye $" + String.format("%.2f", preview.taxAmount()) + " de comisión)");
        }

        UsdToArsConversion conversion = usdToArsConversionService.calculate(amountUsd);

        cuentaUsd.setBalance(cuentaUsd.getBalance() - conversion.totalDebitado());
        cuentaArs.setBalance(cuentaArs.getBalance() + conversion.amountArs());

        Transaction transaction = new Transaction();
        transaction.setIdOrigin(cuentaUsd);
        transaction.setIdDestination(cuentaArs);
        transaction.setBalance(conversion.amountArs());
        transaction.setOriginalAmount(conversion.amountUsd());
        transaction.setOriginalCurrency(Currency.USD);
        transaction.setCurrency(Currency.ARS);
        transaction.setExchangeRate(conversion.exchangeRate());
        transaction.setTaxAmount(conversion.taxAmount());
        transaction.setTaxPercentage(conversion.taxPercentage());
        transaction.setState("COMPLETED");

        accountRepository.save(cuentaUsd);
        accountRepository.save(cuentaArs);
        transactionRepository.save(transaction);

        Event event = new Event(EventType.TRANSACTION_COMPLETED);
        event.addData("user", cuentaUsd.getUser());
        event.addData("amount", conversion.amountArs());
        event.addData("amountUsd", conversion.amountUsd());
        event.addData("exchangeRate", conversion.exchangeRate());
        event.addData("taxAmount", conversion.taxAmount());
        event.addData("taxPercentage", conversion.taxPercentage());
        event.addData("totalDebitado", conversion.totalDebitado());
        event.addData("destinationAlias", cuentaArs.getAccountNickname());
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
                cuentaArs.getBalance(),
                cuentaUsd.getBalance()
        );
    }

    private void persistSellFailed(Account cuentaUsd, Account cuentaArs, double amountUsd) {
        Transaction transaction = new Transaction();
        transaction.setIdOrigin(cuentaUsd);
        transaction.setIdDestination(cuentaArs);
        transaction.setBalance(amountUsd);
        transaction.setState("FAILED");
        transaction.setCurrency(Currency.USD);
        transaction.setOriginalAmount(amountUsd);
        transaction.setOriginalCurrency(Currency.USD);
        transactionRepository.save(transaction);
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
