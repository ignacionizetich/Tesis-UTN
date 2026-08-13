package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.User;

import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.observer.Event;
import com.EDJ.ArCash.observer.EventPublisher;
import com.EDJ.ArCash.observer.EventType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final EventPublisher eventPublisher;
    private final AccountIdentifierGenerator identifierGenerator;
    private final AliasFormatValidator aliasFormatValidator;

    public AccountService(AccountRepository accountRepository,
                          EventPublisher eventPublisher,
                          AccountIdentifierGenerator identifierGenerator,
                          AliasFormatValidator aliasFormatValidator) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
        this.identifierGenerator = identifierGenerator;
        this.aliasFormatValidator = aliasFormatValidator;
    }


    public void createAccount(User user) {
        Account account = new Account();
        account.setUser(user);
        account.setAccountType(Currency.ARS); ///se crea la cuenta como ARS por defecto
        account.setBalance(0.0);
        account.setAccountNickname(identifierGenerator.generateUniqueNickname());
        account.setAccountCvu(identifierGenerator.generateUniqueCvu());
        accountRepository.save(account);
        
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

        Event event = new Event(EventType.USD_ACCOUNT_CREATED);
        event.addData("user", user);
        event.addData("accountAlias", account.getAccountNickname());
        event.addData("accountCvu", account.getAccountCvu());
        eventPublisher.publish(event);

        return account;
    }

    public Account openUsdAccount(User user){
        boolean alreadyHasAccount = accountRepository.existsByUserAndAccountType(user, Currency.USD);

        if(alreadyHasAccount){
            throw new IllegalStateException("El usuario ya cuenta con una cuenta en dolares");
        }

        return createUsdAccount(user);
    }

    /**
     * Ingreso de dinero con ownership y relectura de saldo.
     * updateBalance(boolean) sigue siendo el primitivo de mutacion.
     */
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

    /**
     * Saldo de una cuenta propia. Empty si no existe o no es del usuario
     * (el controller responde el mismo 403 en ambos casos).
     */
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





