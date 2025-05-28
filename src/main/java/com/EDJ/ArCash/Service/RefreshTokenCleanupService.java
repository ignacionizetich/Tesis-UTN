package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
@Slf4j
@Service
public class RefreshTokenCleanupService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @PostConstruct
    public void init() {
        // Se ejecuta inmediatamente al iniciar
        cleanupExpiredTokens();
    }

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        
        System.out.println("Buscando refresh tokens expirados antes de: " + now);
        
        try {
            int deletedTokens = refreshTokenRepository.deleteByRevokedTrueOrExpiresAtBefore(now);
            
            System.out.println("refresh tokens encontrados: " + deletedTokens);
            System.out.println("refresh tokens eliminado:[" + (deletedTokens > 0 ? "OK" : "") + "]");
            
        } catch (Exception e) {
            System.out.println("Error durante la limpieza de refresh tokens: " + e.getMessage());
        }
    }
}