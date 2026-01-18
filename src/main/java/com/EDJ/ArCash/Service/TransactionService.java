package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.DTO.AuthDTO.TransactionDTO;
import com.EDJ.ArCash.Models.Account;
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

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository, EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public boolean transaction(Long idOrigen, Long idDestino, double monto) {
        if (monto <= 0) {
            return false;
        }

        Optional<Account> optionalOrigen = accountRepository.findByIdAccount(idOrigen);
        Optional<Account> optionalDestino = accountRepository.findByIdAccount(idDestino);

        if (optionalOrigen.isEmpty() || optionalDestino.isEmpty()) {
            return false;
        }
        Account cuentaOrigen = optionalOrigen.get();
        Account cuentaDestino = optionalDestino.get();
        Transaction transaction = new Transaction();

        // Transferencia a uno mismo: marcar como fallida, no mover dinero
        if (idOrigen.equals(idDestino)) {
            transaction.setIdOrigin(cuentaOrigen);
            transaction.setIdDestination(cuentaDestino);
            transaction.setBalance(monto);
            transaction.setState("FAILED");
            transactionRepository.save(transaction);
            return true; // Se completa, pero como fallida
        }

        if (cuentaOrigen.getBalance() < monto) {
            transaction.setIdOrigin(cuentaOrigen);
            transaction.setIdDestination(cuentaDestino);
            transaction.setBalance(monto);
            transaction.setState("FAILED");
            transactionRepository.save(transaction);
            return false;
        } else {
            cuentaOrigen.setBalance(cuentaOrigen.getBalance() - monto);
            cuentaDestino.setBalance(cuentaDestino.getBalance() + monto);
            transaction.setIdOrigin(cuentaOrigen);
            transaction.setIdDestination(cuentaDestino);
            transaction.setBalance(monto);
            transaction.setState("COMPLETED");
            accountRepository.save(cuentaOrigen);
            accountRepository.save(cuentaDestino);
            transactionRepository.save(transaction);
            
            // Publicar evento de transacción completada
            Event event = new Event(EventType.TRANSACTION_COMPLETED);
            event.addData("user", cuentaOrigen.getUser());
            event.addData("amount", monto);
            event.addData("destinationAlias", cuentaDestino.getAccountNickname());
            eventPublisher.publish(event);
            
            return true;
        }
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