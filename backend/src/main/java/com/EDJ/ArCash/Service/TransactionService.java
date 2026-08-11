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

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final EventPublisher eventPublisher;
    private final ArsToUsdConversionService arsToUsdConversionService;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              EventPublisher eventPublisher,
                              ArsToUsdConversionService arsToUsdConversionService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
        this.arsToUsdConversionService = arsToUsdConversionService;
    }

    @Transactional
    public boolean transaction(Long idOrigen, Long idDestino, double monto) {
        return (boolean) transactionWithDetails(idOrigen, idDestino, monto).get("success");
    }
    
    @Transactional
    public Map<String, Object> transactionWithDetails(Long idOrigen, Long idDestino, double monto) {
        Map<String, Object> result = new HashMap<>();
        
        if (monto <= 0) {
            result.put("success", false);
            result.put("message", "El monto debe ser mayor a cero");
            return result;
        }

        Optional<Account> optionalOrigen = accountRepository.findByIdAccount(idOrigen);
        Optional<Account> optionalDestino = accountRepository.findByIdAccount(idDestino);

        if (optionalOrigen.isEmpty() || optionalDestino.isEmpty()) {
            result.put("success", false);
            result.put("message", "Cuenta no encontrada");
            return result;
        }
        
        Account cuentaOrigen = optionalOrigen.get();
        Account cuentaDestino = optionalDestino.get();
        
        // Verificar si las cuentas son de la misma moneda
        if (cuentaOrigen.getAccountType() != cuentaDestino.getAccountType()) {
            // Transferencia con conversión de moneda
            return transactionWithConversionDetails(cuentaOrigen, cuentaDestino, monto);
        }
        
        // Transferencia sin conversión (mismo tipo de moneda)
        boolean success = transactionSameCurrency(cuentaOrigen, cuentaDestino, monto);
        result.put("success", success);
        if (!success) {
            result.put("message", "Saldo insuficiente o error en la transacción");
        }
        return result;
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
            return true; // Se completa, pero como fallida
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
            
            // Publicar evento de transacción completada
            Event event = new Event(EventType.TRANSACTION_COMPLETED);
            event.addData("user", cuentaOrigen.getUser());
            event.addData("amount", monto);
            event.addData("destinationAlias", cuentaDestino.getAccountNickname());
            event.addData("currency", cuentaOrigen.getAccountType().toString());
            event.addData("converted", false);
            eventPublisher.publish(event);
            
            return true;
        }
    }
    
    @Transactional
    public Map<String, Object> transactionWithConversionDetails(Account cuentaOrigen, Account cuentaDestino, double monto) {
        Map<String, Object> result = new HashMap<>();
        Transaction transaction = new Transaction();
        
        // Solo permitir conversión de ARS a USD
        if (cuentaOrigen.getAccountType() != Currency.ARS || cuentaDestino.getAccountType() != Currency.USD) {
            transaction.setIdOrigin(cuentaOrigen);
            transaction.setIdDestination(cuentaDestino);
            transaction.setBalance(monto);
            transaction.setState("FAILED");
            transaction.setCurrency(cuentaOrigen.getAccountType());
            transactionRepository.save(transaction);
            result.put("success", false);
            result.put("message", "Solo se permite conversión de ARS a USD");
            return result;
        }

        DebitPreview preview = arsToUsdConversionService.previewDebit(monto);

        // Verificar saldo suficiente (monto + comision) ANTES de consultar cotizacion
        if (cuentaOrigen.getBalance() < preview.totalDebitado()) {
            transaction.setIdOrigin(cuentaOrigen);
            transaction.setIdDestination(cuentaDestino);
            transaction.setBalance(monto);
            transaction.setState("FAILED");
            transaction.setCurrency(Currency.ARS);
            transaction.setOriginalAmount(monto);
            transaction.setOriginalCurrency(Currency.ARS);
            transactionRepository.save(transaction);

            result.put("success", false);
            result.put("message", String.format("Saldo insuficiente. Para enviar $%.2f ARS necesitas $%.2f ARS (incluye $%.2f de comisión). Tu saldo actual es $%.2f ARS",
                    monto, preview.totalDebitado(), preview.taxAmount(), cuentaOrigen.getBalance()));
            result.put("montoRequerido", preview.totalDebitado());
            result.put("saldoActual", cuentaOrigen.getBalance());
            result.put("impuestos", preview.taxAmount());
            return result;
        }

        ArsToUsdConversion conversion = arsToUsdConversionService.calculate(monto);

        // Debitar monto + comision; acreditar solo amountUsd (comision no convertida)
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
        eventPublisher.publish(event);
        
        result.put("success", true);
        result.put("message", "Transferencia completada exitosamente");
        return result;
    }
    
    @Transactional
    public boolean transactionWithConversion(Account cuentaOrigen, Account cuentaDestino, double monto) {
        return (boolean) transactionWithConversionDetails(cuentaOrigen, cuentaDestino, monto).get("success");
    }
    
    @Transactional
    public Map<String, Object> buyUsd(Long accountArsId, Long accountUsdId, double amountArs) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<Account> optionalArs = accountRepository.findByIdAccount(accountArsId);
        Optional<Account> optionalUsd = accountRepository.findByIdAccount(accountUsdId);
        
        if (optionalArs.isEmpty() || optionalUsd.isEmpty()) {
            result.put("success", false);
            result.put("message", "Cuenta no encontrada");
            return result;
        }
        
        Account cuentaArs = optionalArs.get();
        Account cuentaUsd = optionalUsd.get();
        
        // Validar que sean del mismo usuario
        if (!cuentaArs.getUser().getId().equals(cuentaUsd.getUser().getId())) {
            result.put("success", false);
            result.put("message", "Las cuentas deben pertenecer al mismo usuario");
            return result;
        }
        
        // Validar tipos de cuenta
        if (cuentaArs.getAccountType() != Currency.ARS || cuentaUsd.getAccountType() != Currency.USD) {
            result.put("success", false);
            result.put("message", "Debe comprar desde una cuenta en pesos a una cuenta en dólares");
            return result;
        }

        DebitPreview preview = arsToUsdConversionService.previewDebit(amountArs);

        if (cuentaArs.getBalance() < preview.totalDebitado()) {
            result.put("success", false);
            result.put("message", "Saldo insuficiente. Necesitas $" + String.format("%.2f", preview.totalDebitado()) +
                                 " ARS (incluye $" + String.format("%.2f", preview.taxAmount()) + " de comisión)");
            return result;
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
        
        result.put("success", true);
        result.put("message", "Compra de dólares exitosa");
        result.put("amountArs", conversion.amountArs());
        result.put("amountUsd", conversion.amountUsd());
        result.put("exchangeRate", conversion.exchangeRate());
        result.put("taxAmount", conversion.taxAmount());
        result.put("taxPercentage", conversion.taxPercentage());
        result.put("totalDebitado", conversion.totalDebitado());
        result.put("newBalanceArs", cuentaArs.getBalance());
        result.put("newBalanceUsd", cuentaUsd.getBalance());
        
        return result;
    }

    public List<TransactionDTO> listaTransacciones(Long id) {
        Optional<Account> accountOptional = accountRepository.findByIdAccount(id);
        if (accountOptional.isPresent()) {
            List<Transaction> lista = transactionRepository.findByIdOriginOrIdDestination(accountOptional.get(), accountOptional.get());
            // Ordenar por fecha descendente (más reciente primero)
            lista.sort((t1, t2) -> t2.getTransaction_date().compareTo(t1.getTransaction_date()));
            return lista.stream()
                    .map(TransactionDTO::new)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
