package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.CardUnlockService;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CardUnlockServiceImpl implements CardUnlockService {

    private static final long TTL_SECONDS = 5 * 60;

    private final Map<String, UnlockSession> sessions = new ConcurrentHashMap<>();

    public String createUnlock(Long userId) {
        purgeExpired();
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new UnlockSession(userId, Instant.now().plusSeconds(TTL_SECONDS)));
        return token;
    }

    public boolean isValid(String token, Long userId) {
        if (token == null || token.isBlank()) {
            return false;
        }
        UnlockSession session = sessions.get(token);
        if (session == null) {
            return false;
        }
        if (Instant.now().isAfter(session.expiresAt())) {
            sessions.remove(token);
            return false;
        }
        return session.userId().equals(userId);
    }

    public void revoke(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }

    private record UnlockSession(Long userId, Instant expiresAt) {}
}
