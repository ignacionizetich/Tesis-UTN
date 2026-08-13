package com.EDJ.ArCash.Service.support;

import com.EDJ.ArCash.Models.Imp.Currency;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;

@Component
public class CardNumberGenerator {

    /** BIN ficticio Arcash  */
    private static final String BIN_ARS = "539912";
    private static final String BIN_USD = "539913";

    private final SecureRandom random = new SecureRandom();

    public GeneratedCardNumbers generate(Currency currency) {
        String bin = currency == Currency.USD ? BIN_USD : BIN_ARS;
        StringBuilder body = new StringBuilder(bin);
        while (body.length() < 15) {
            body.append(random.nextInt(10));
        }
        String panWithoutCheck = body.toString();
        int check = luhnCheckDigit(panWithoutCheck);
        String pan = panWithoutCheck + check;
        String last4 = pan.substring(pan.length() - 4);
        String cvc = String.format("%03d", random.nextInt(1000));
        LocalDate exp = LocalDate.now().plusYears(4);
        return new GeneratedCardNumbers(pan, last4, cvc, exp.getMonthValue(), exp.getYear());
    }

    private int luhnCheckDigit(String numberWithoutCheck) {
        int sum = 0;
        boolean alternate = true;
        for (int i = numberWithoutCheck.length() - 1; i >= 0; i--) {
            int n = numberWithoutCheck.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }

    public record GeneratedCardNumbers(
            String pan,
            String last4,
            String cvc,
            int expMonth,
            int expYear
    ) {}
}
