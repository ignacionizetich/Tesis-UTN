package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.VirtualCardService;
import com.EDJ.ArCash.Service.interfaces.AccountService;
import com.EDJ.ArCash.Service.result.*;
import com.EDJ.ArCash.Service.support.*;

import com.EDJ.ArCash.DTO.AuthDTO.AccountSearchResponse;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.User;

import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.observer.Event;
import com.EDJ.ArCash.observer.EventPublisher;
import com.EDJ.ArCash.observer.EventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final EventPublisher eventPublisher;
    private final AccountIdentifierGenerator identifierGenerator;
    private final AliasFormatValidator aliasFormatValidator;
    private final VirtualCardService virtualCardService;



    public void createAccount(User user) {
        Account account = new Account();
        account.setUser(user);
        account.setAccountType(Currency.ARS); ///se crea la cuenta como ARS por defecto
        account.setBalance(0.0);
        account.setAccountNickname(identifierGenerator.generateUniqueNickname());
        account.setAccountCvu(identifierGenerator.generateUniqueCvu());
        accountRepository.save(account);
        virtualCardService.createForAccount(account);

        // Publicar evento de cuenta creada
        Event event = new Event(EventType.ACCOUNT_CREATED);
        event.addData("user", user);
        event.addData("accountAlias", account.getAccountNickname());
        event.addData("accountCvu", account.getAccountCvu());
        eventPublisher.publish(event);
    }

    public Account createUsdAccount(User user){
        Account account = new Account();
        account.setUser(user);
        account.setAccountType(Currency.USD);
        account.setBalance(0.0);
        account.setAccountNickname(identifierGenerator.generateUniqueNickname());
        account.setAccountCvu(identifierGenerator.generateUniqueCvu());

        accountRepository.save(account);
        virtualCardService.createForAccount(account);

        Event event = new Event(EventType.USD_ACCOUNT_CREATED);
        event.addData("user", user);
        event.addData("accountAlias", account.getAccountNickname());
        event.addData("accountCvu", account.getAccountCvu());
        eventPublisher.publish(event);

        return account;
    }

    public Account ensureArsAccount(User user) {
        Optional<Account> existing = accountRepository.findArsAccountByUserId(user.getId(), Currency.ARS);
        if (existing.isPresent()) {
            return existing.get();
        }
        createAccount(user);
        return accountRepository.findArsAccountByUserId(user.getId(), Currency.ARS)
                .orElseThrow(() -> new IllegalStateException(
                        "No se pudo crear la cuenta ARS para el usuario " + user.getId()));
    }

    public OpenUsdResult openUsdAccount(User user) {
        try {
            boolean alreadyHasAccount = accountRepository.existsByUserAndAccountType(user, Currency.USD);
            if (alreadyHasAccount) {
                return OpenUsdResult.alreadyExists();
            }
            return OpenUsdResult.ok(createUsdAccount(user));
        } catch (Exception e) {
            return OpenUsdResult.error(e.getMessage());
        }
    }

    public DepositResult deposit(Long accountId, Long userId, double amount) {
        if (amount < 0) {
            return DepositResult.montoNegativo(amount);
        }

        Optional<Account> optionalAccount = accountRepository.findByIdAccount(accountId);
        if (optionalAccount.isEmpty()) {
            return DepositResult.cuentaNoExiste();
        }

        Account account = optionalAccount.get();
        if (!account.getUser().getId().equals(userId)) {
            return DepositResult.noEsPropietario();
        }

        if (!updateBalance(amount, accountId)) {
            return DepositResult.updateFallido();
        }

        Optional<Account> actualizada = accountRepository.findByIdAccount(accountId);
        if (actualizada.isEmpty()) {
            return DepositResult.updateFallido();
        }

        return DepositResult.ok(actualizada.get().getBalance());
    }

    public Optional<AccountBalanceView> getOwnedBalance(Long accountId, Long userId) {
        Optional<Account> optionalAccount = accountRepository.findByIdAccount(accountId);
        if (optionalAccount.isEmpty()) {
            return Optional.empty();
        }
        Account account = optionalAccount.get();
        if (!account.getUser().getId().equals(userId)) {
            return Optional.empty();
        }
        return Optional.of(new AccountBalanceView(
                account.getBalance(),
                account.getAccountNickname(),
                account.getAccountCvu()
        ));
    }

    public QrDataResult getQrDataForOwner(Long accountId, Long userId) {
        Optional<Account> optionalAccount = accountRepository.findByIdAccount(accountId);
        if (optionalAccount.isEmpty()) {
            return QrDataResult.cuentaNoEncontrada();
        }

        Account account = optionalAccount.get();
        if (!account.getUser().getId().equals(userId)) {
            return QrDataResult.noEsPropietario();
        }

        User user = account.getUser();
        String currency = "ARS".equalsIgnoreCase(account.getAccountType().toString())
                ? "ARS"
                : account.getAccountType().toString();

        return QrDataResult.ok(new QrDataResult.QrPayload(
                "ArCashV1",
                account.getIdAccount(),
                account.getAccountNickname(),
                user.getName() + " " + user.getLastName(),
                user.getDni(),
                user.getEmail(),
                currency
        ));
    }

    public boolean updateBalance(double balanceToAdd, Long id){
        Optional<Account> optionalAccount = accountRepository.findByIdAccount(id);

        if (optionalAccount.isEmpty()) {
            return false;
        } else {
            Account account = optionalAccount.get();
            double newBalance = account.getBalance() + balanceToAdd;
            account.setBalance(newBalance);
            accountRepository.save(account);
            return true;
        }
    }

   public Optional<Account> findAccountByID(long id){
        return accountRepository.findByIdAccount(id);
    }

    public List<Account> findAccountsByUser(Long userId){
        return accountRepository.findAllByUser_Id(userId);
    }

    public Optional<Account> encontrarCuentaPorAlias(String alias){
        return accountRepository.findByAccountNickname(alias);
    }

    public Optional<Account> encontrarCuentaPorCvu(String cvu){
        return accountRepository.findByAccountCvu(cvu);
    }

    public Optional<AccountSearchResponse> searchByAliasOrCvu(String input) {
        Optional<Account> account = encontrarCuentaPorAlias(input);
        if (account.isEmpty()) {
            account = encontrarCuentaPorCvu(input);
        }
        return account.map(this::toSearchResponse);
    }

    private AccountSearchResponse toSearchResponse(Account acc) {
        AccountSearchResponse.UserSummary user = new AccountSearchResponse.UserSummary(
                acc.getUser().getName(),
                acc.getUser().getLastName(),
                acc.getUser().getDni()
        );
        return new AccountSearchResponse(
                acc.getIdAccount(),
                acc.getAccountNickname(),
                acc.getAccountCvu(),
                acc.getAccountType() != null ? acc.getAccountType().name() : null,
                user
        );
    }

    public AliasChangeResult changeAlias(String newAlias, Long id, Long userId) {
        if (!aliasFormatValidator.esValido(newAlias)) {
            return AliasChangeResult.FORMATO_INVALIDO;
        }

        Optional<Account> optionalAccount = accountRepository.findByIdAccount(id);

        if (optionalAccount.isEmpty()) {
            return AliasChangeResult.CUENTA_NO_ENCONTRADA;
        }
        Account acc = optionalAccount.get();

        if (!acc.getUser().getId().equals(userId)) {
            return AliasChangeResult.NO_ES_PROPIETARIO;
        }

        if (accountRepository.existsByAccountNickname(newAlias)) {
            return AliasChangeResult.ALIAS_EN_USO;
        }

        String oldAlias = acc.getAccountNickname();
        acc.setAccountNickname(newAlias);
        accountRepository.save(acc);

        // Publicar evento de cambio de alias
        Event event = new Event(EventType.ALIAS_CHANGED);
        event.addData("user", acc.getUser());
        event.addData("oldAlias", oldAlias);
        event.addData("newAlias", newAlias);
        eventPublisher.publish(event);

        return AliasChangeResult.OK;
    }
}

