package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.RefreshToken;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RefreshTokenCleanupService implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    @Lazy
    private RefreshTokenCleanupService self; // Inyecta el propio bean

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        self.removeExpiredOrRevokedTokens(); // Llama a través del proxy
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * ?")
    public void removeExpiredOrRevokedTokens() {
        int deleted = refreshTokenRepository.deleteByRevokedTrueOrExpiresAtBefore(LocalDateTime.now());
        System.out.println("Refresh tokens eliminados: " + deleted);
    }

    public Optional<RefreshToken> getRefreshTokenAndRevokedFalse(String token){
        return this.refreshTokenRepository.findByRefreshTokenAndRevokedFalse(token);
    }
}