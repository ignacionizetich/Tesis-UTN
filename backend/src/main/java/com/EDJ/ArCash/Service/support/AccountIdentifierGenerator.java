package com.EDJ.ArCash.Service.support;

import com.EDJ.ArCash.Repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class AccountIdentifierGenerator {

    private static final String[] PALABRAS = {
            "happy", "brave", "fast", "calm", "smart", "silly", "cool", "kind", "wild", "bold",
            "tiger", "lion", "panda", "eagle", "fox", "whale", "zebra", "wolf", "rabbit", "koala",
            "red", "green", "blue", "yellow", "black", "white", "pink", "orange", "purple", "brown",
            "apple", "orange", "banana", "grapes", "peach", "pear", "strawberry", "cherry", "mango", "melon"
    };

    private static final int LARGO_MAXIMO_ALIAS = 25;
    private static final String CODIGO_ENTIDAD = "00002001";
    private static final int DIGITOS_DE_CUENTA = 13;
    private static final int[] PESOS_VERIFICADOR = {3, 1};

    private final AccountRepository accountRepository;
    private final Random random = new Random();

    public AccountIdentifierGenerator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public String generateUniqueNickname() {
        String nickname;
        do {
            nickname = generateRandomNickname();
        } while (accountRepository.existsByAccountNickname(nickname));
        return nickname;
    }

    public String generateUniqueCvu() {
        String cvu;
        do {
            cvu = generateCvu();
        } while (accountRepository.existsByAccountCvu(cvu));
        return cvu;
    }

    private String generateRandomNickname() {
        String primera = PALABRAS[random.nextInt(PALABRAS.length)];
        String segunda = PALABRAS[random.nextInt(PALABRAS.length)];

        char sufijo1 = (char) ('A' + random.nextInt(26));
        char sufijo2 = (char) ('A' + random.nextInt(26));

        String alias = primera + "." + segunda + "." + sufijo1 + sufijo2;
        return alias.length() > LARGO_MAXIMO_ALIAS
                ? alias.substring(0, LARGO_MAXIMO_ALIAS).toUpperCase()
                : alias.toUpperCase();
    }

    private String generateCvu() {
        String base = CODIGO_ENTIDAD + generateRandomDigits(DIGITOS_DE_CUENTA);
        return base + calculateValidatorDigit(base);
    }

    private String generateRandomDigits(int length) {
        StringBuilder digitos = new StringBuilder();
        for (int i = 0; i < length; i++) {
            digitos.append(random.nextInt(10));
        }
        return digitos.toString();
    }

    private int calculateValidatorDigit(String base) {
        int suma = 0;
        for (int i = 0; i < base.length(); i++) {
            suma += Character.getNumericValue(base.charAt(i)) * PESOS_VERIFICADOR[i % 2];
        }
        int resto = suma % 10;
        return resto == 0 ? 0 : 10 - resto;
    }
}
