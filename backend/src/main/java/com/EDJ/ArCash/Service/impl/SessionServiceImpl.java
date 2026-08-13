package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.SessionService;

import com.EDJ.ArCash.Models.RefreshToken;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SessionServiceImpl implements SessionService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public SessionServiceImpl(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
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
