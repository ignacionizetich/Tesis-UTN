package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.RefreshToken;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Estado de sesion basado en refresh tokens: saber si hay una sesion vigente
 * y cerrarla (revocar todos los refresh activos del usuario).
 */
@Service
public class SessionService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public SessionService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public boolean tieneSesionActiva(Long userId) {
        return userRepository.findById(userId)
                .flatMap(refreshTokenRepository::findByUserAndRevokedFalse)
                .isPresent();
    }

    public void revokeAllUserTokens(Long userId) {
        Optional<User> optional = userRepository.findById(userId);
        if (optional.isEmpty()) {
            return;
        }

        User user = optional.get();
        List<RefreshToken> validUserTokens = refreshTokenRepository.findAllByUserAndRevokedFalse(user);
        if (validUserTokens.isEmpty()) {
            return;
        }

        validUserTokens.forEach(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }
}
