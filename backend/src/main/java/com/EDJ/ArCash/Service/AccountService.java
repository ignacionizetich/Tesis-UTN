package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.User;

import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.ValidationTokenRepository;
import com.EDJ.ArCash.observer.Event;
import com.EDJ.ArCash.observer.EventPublisher;
import com.EDJ.ArCash.observer.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
public class AccountService {
    @Autowired
    private final ValidationTokenRepository validationTokenRepository;

    @Autowired
    private final AccountRepository accountRepository;

    @Autowired
    private final EventPublisher eventPublisher;

    public AccountService(AccountRepository accountRepository, ValidationTokenRepository validationTokenRepository, EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.validationTokenRepository = validationTokenRepository;
        this.eventPublisher = eventPublisher;
    }


    public void createAccount(User user) {
        Account account = new Account();
        account.setUser(user);
        account.setAccountType(Currency.ARS); ///se crea la cuenta como ARS por defecto
        account.setBalance(0.0);
        account.setAccountNickname(generateUniqueNickname());
        account.setAccountCvu(generateUniqueCvu());
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
        account.setAccountNickname(generateUniqueNickname());
        account.setAccountCvu(generateUniqueCvu());

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

    public Optional<Account> encontrarCuentaPorAlias(String alias){
        return accountRepository.findByAccountNickname(alias);
    }

    public Optional<Account> encontrarCuentaPorCvu(String cvu){
        return accountRepository.findByAccountCvu(cvu);
    }

    public AliasChangeResult changeAlias(String newAlias, Long id, Long userId) {
        // Regex mejorado: exige al menos una letra
        String regex = "^(?=.*[A-Za-z])(?=^[A-Za-z0-9]+(\\.[A-Za-z0-9]+)+$)(?!.*\\.\\.)[A-Za-z0-9.]{4,25}$";
        if (!newAlias.matches(regex)) {
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





    /// -----------------------METODOS PRIVATE PARA GENERAR UN ALIAS ALEATORIO Y EL CVU DE LA CUENTA -----------------------

    private String generateUniqueNickname() {
        String account_nickname;
        do {
            account_nickname = generateRandomNickname();
        } while (accountRepository.existsByAccountNickname(account_nickname));
        return account_nickname;
    }

    private String generateRandomNickname() {
        String[] options = {"happy", "brave", "fast", "calm", "smart", "silly", "cool", "kind", "wild", "bold",
                "tiger", "lion", "panda", "eagle", "fox", "whale", "zebra", "wolf", "rabbit", "koala",
                "red", "green", "blue", "yellow", "black", "white", "pink", "orange", "purple", "brown",
                "apple", "orange", "banana", "grapes", "peach", "pear", "strawberry", "cherry", "mango", "melon"
        };
        Random rand = new Random();

        String first = options[rand.nextInt(options.length)];
        String second = options[rand.nextInt(options.length)];


        char sufijo1 = (char) ('A' + rand.nextInt(26));
        char sufijo2 = (char) ('A' + rand.nextInt(26));


        String alias = (first + "." + second + "." + sufijo1 + sufijo2);
        return alias.length() > 25 ? alias.substring(0, 25).toUpperCase() : alias.toUpperCase();
    }

    private String generateUniqueCvu() {
        String account_cvu;
        do {
            account_cvu = generateCvu();
        } while (accountRepository.existsByAccountCvu(account_cvu));
        return account_cvu;
    }

    private String generateCvu() {
        String entidad = "00002001";
        String cuenta = generateRandomDigits(13);
        String base = entidad + cuenta;
        int verificador = calculateValidatorDigit(base);
        return base + verificador;
    }

    private String generateRandomDigits(int length) {
        Random rand = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(rand.nextInt(10));
        }
        return sb.toString();
    }

    private int calculateValidatorDigit(String base) {
        int[] pesos = {3, 1};
        int suma = 0;
        for (int i = 0; i < base.length(); i++) {
            suma += Character.getNumericValue(base.charAt(i)) * pesos[i % 2];
        }
        int resto = suma % 10;
        return resto == 0 ? 0 : 10 - resto;
    }
}





