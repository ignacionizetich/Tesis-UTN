package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Service.interfaces.MetricsService;
import com.EDJ.ArCash.DTO.AuthDTO.AdminMetricsResponse;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Imp.Currency;
import com.EDJ.ArCash.Models.Imp.Permissions;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MetricsServiceTest {

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @DisplayName("collect incluye usuarios y saldos ARS")
    void collectIncluyeUsuariosYSaldos() {
        User user = newUser("metrics.user.a");
        user = userRepository.save(user);

        Account ars = new Account();
        ars.setUser(user);
        ars.setAccountType(Currency.ARS);
        ars.setBalance(1500.50);
        ars.setAccountCvu("0000003100000000000099");
        ars.setAccountNickname("metrics.user.a.ars");
        accountRepository.save(ars);

        AdminMetricsResponse metrics = metricsService.collect();

        assertNotNull(metrics.summary());
        assertTrue(metrics.summary().totalUsers() >= 1);
        assertTrue(metrics.summary().totalBalanceArs() >= 1500.50);
        assertEquals(14, metrics.registrationsLast14Days().size());
        assertEquals(14, metrics.transactionsLast14Days().size());
        assertNotNull(metrics.generatedAt());
    }

    private User newUser(String alias) {
        User user = new User("Metrics", "Test", "dni-" + alias, alias + "@test.local", alias);
        user.setEnabled(true);
        user.setActive(true);
        user.setPermissions(Permissions.USER);
        return user;
    }
}
