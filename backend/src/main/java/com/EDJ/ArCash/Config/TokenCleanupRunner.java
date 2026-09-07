package com.EDJ.ArCash.Config;

import com.EDJ.ArCash.Service.interfaces.TokenCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Purga los tokens ya consumidos y los usuarios que nunca validaron su email.
 *
 * <p>Se dispara con {@link ApplicationReadyEvent} y no dentro del propio servicio: invocar el
 * metodo desde el bean que lo declara saltea el proxy de {@code @Transactional}, y auto-inyectarse
 * para evitarlo genera un ciclo de dependencias que impide arrancar el contexto.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupRunner {

    private final JdbcTemplate jdbcTemplate;
    private final TokenCleanupService tokenCleanupService;

    @EventListener(ApplicationReadyEvent.class)
    public void purgeOnStartup() {
        purgeConsumedTokens();
        purgeUnvalidatedUsers();
    }

    private void purgeConsumedTokens() {
        try {
            int recovery = jdbcTemplate.update("DELETE FROM recovery_tokens WHERE used = true");
            int refresh = jdbcTemplate.update("DELETE FROM refresh_tokens WHERE revoked = true");
            int validation = jdbcTemplate.update("DELETE FROM validation_tokens WHERE used = true");
            log.info("Tokens consumidos purgados: recovery={}, refresh={}, validation={}",
                    recovery, refresh, validation);
        } catch (Exception e) {
            log.error("No se pudieron purgar los tokens consumidos (se continua): {}", e.getMessage(), e);
        }
    }

    private void purgeUnvalidatedUsers() {
        try {
            tokenCleanupService.removeExpiredUnvalidatedUsers();
        } catch (Exception e) {
            log.error("Cleanup de usuarios no validados fallo en el arranque (se continua): {}",
                    e.getMessage(), e);
        }
    }
}
