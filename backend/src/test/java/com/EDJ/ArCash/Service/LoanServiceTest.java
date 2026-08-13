package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Service.interfaces.LoanRateConfigService;
import com.EDJ.ArCash.Service.interfaces.LoanService;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.DTO.AuthDTO.LoanRatesUpdateRequest;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.LoanRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class LoanServiceTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanRateConfigService loanRateConfigService;

    @Test
    @DisplayName("Simulación: más cuotas implica mayor tasa")
    void simulateCalculaCuotaYTasaDinamica() {
        setRate(3, 3.0);
        setRate(12, 5.5);
        LoanService.SimulationResult shortTerm = loanService.simulate(12_000, 3);
        LoanService.SimulationResult longTerm = loanService.simulate(12_000, 12);
        assertTrue(longTerm.monthlyRate() > shortTerm.monthlyRate());
        assertTrue(shortTerm.installmentAmount() > 12_000.0 / 3);
        assertEquals(3, shortTerm.schedule().size());
    }

    @Test
    @DisplayName("Aceptar acredita ARS y pagar cuota debita")
    void acceptYPayAfectanSaldo() {
        User user = userRepository.save(newUser("loan.user.a"));
        Account ars = new Account();
        ars.setUser(user);
        ars.setAccountType(Currency.ARS);
        ars.setBalance(0);
        ars.setAccountNickname("loan.ars.a");
        ars.setAccountCvu("cvuloana0001");
        ars = accountRepository.save(ars);

        var loan = loanService.accept(user, 10_000, 3);
        Account afterCredit = accountRepository.findByIdAccount(ars.getIdAccount()).orElseThrow();
        assertEquals(10_000.0, afterCredit.getBalance(), 0.01);

        loanService.payNext(loan);
        Account afterPay = accountRepository.findByIdAccount(ars.getIdAccount()).orElseThrow();
        assertTrue(afterPay.getBalance() < 10_000.0);
    }

    @Test
    @DisplayName("No permite un segundo préstamo activo")
    void noSegundoActivo() {
        User user = userRepository.save(newUser("loan.user.b"));
        Account ars = new Account();
        ars.setUser(user);
        ars.setAccountType(Currency.ARS);
        ars.setBalance(50_000);
        ars.setAccountNickname("loan.ars.b");
        ars.setAccountCvu("cvuloanb0002");
        accountRepository.save(ars);

        loanService.accept(user, 5_000, 3);
        assertThrows(IllegalStateException.class, () -> loanService.accept(user, 5_000, 3));
    }

    private static User newUser(String alias) {
        User user = new User("Loan", "Test", "dni-" + alias, alias + "@test.local", alias);
        user.setPermissions(Permissions.USER);
        user.setEnabled(true);
        user.setActive(true);
        return user;
    }

    private void setRate(int installments, double percent) {
        LoanRatesUpdateRequest request = new LoanRatesUpdateRequest();
        LoanRatesUpdateRequest.LoanRateUpdateItem item = new LoanRatesUpdateRequest.LoanRateUpdateItem();
        item.setInstallments(installments);
        item.setMonthlyRatePercent(percent);
        request.setRates(List.of(item));
        loanRateConfigService.updateRates(request);
    }
}
