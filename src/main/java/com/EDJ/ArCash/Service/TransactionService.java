package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Transaction;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TransactionService {


    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
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
        if (cuentaOrigen.getBalance() < monto) {
            transaction.setId_origin(cuentaOrigen);
            transaction.setId_destination(cuentaDestino);
            transaction.setBalance(monto);
            transaction.setState("FAILED");
            transactionRepository.save(transaction);
            return false;
        } else {
            cuentaOrigen.setBalance(cuentaOrigen.getBalance() - monto);
            cuentaDestino.setBalance(cuentaDestino.getBalance() + monto);
            transaction.setId_origin(cuentaOrigen);
            transaction.setId_destination(cuentaDestino);
            transaction.setBalance(monto);
            transaction.setState("COMPLETED");
            accountRepository.save(cuentaOrigen);
            accountRepository.save(cuentaDestino);
            transactionRepository.save(transaction);
            return true;
        }
    }

}
