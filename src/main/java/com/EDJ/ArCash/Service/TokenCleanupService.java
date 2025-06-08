package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.ValidationToken;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.Repository.ValidationTokenRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class TokenCleanupService implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private ValidationTokenRepository validationTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private TokenCleanupService self; // Inyecta el propio bean

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        self.removeExpiredUnvalidatedUsers(); // Llama a través del proxy
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * ?")
    public void removeExpiredUnvalidatedUsers() {
        LocalDateTime now = LocalDateTime.now();
        System.out.println("Buscando tokens expirados antes de: " + now);

        List<ValidationToken> expiredTokensAndUsers = validationTokenRepository.findAllByUsedFalseAndExpirationDateBefore(now);
        System.out.println("Tokens encontrados: " + expiredTokensAndUsers.size());

        for (ValidationToken token : expiredTokensAndUsers) {
            userRepository.delete(token.getUser());
        }

        List<ValidationToken> expiredTokens = validationTokenRepository.findAllByUsedTrueAndExpirationDateBefore(now);
        validationTokenRepository.deleteAllInBatch(expiredTokens);
        System.out.println("Token eliminado:[" + (!expiredTokens.isEmpty() ? "OK" : "") + "]");
    }

}