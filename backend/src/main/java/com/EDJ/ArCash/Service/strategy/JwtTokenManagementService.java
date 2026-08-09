package com.EDJ.ArCash.Service.strategy;

import com.EDJ.ArCash.Models.Imp.LogoutStatus;
import com.EDJ.ArCash.Models.RefreshToken;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.Security.JwtService;
import com.EDJ.ArCash.Service.SessionService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de TokenManagementStrategy usando JWT
 * Responsable únicamente de la gestión de tokens de autenticación
 */
@Service("jwtTokenManagementService")
public class JwtTokenManagementService implements TokenManagementStrategy {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenManagementService.class);

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public String generateAccessToken(String userId, String role) {
        logger.debug("Generando access token para usuario: {}", userId);
        return jwtService.generateToken(userId, role);
    }

    @Override
    public String generateRefreshToken(String userId, String role) {
        logger.debug("Generando refresh token para usuario: {}", userId);
        return jwtService.generateRefreshToken(userId, role);
    }

    @Override
    @Transactional
    public void saveRefreshToken(User user, String refreshToken) {
        logger.debug("Guardando refresh token para usuario: {}", user.getId());
        
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setRefreshToken(refreshToken);
        token.setIssuedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        token.setRevoked(false);
        
        refreshTokenRepository.save(token);
        logger.info("Refresh token guardado exitosamente para usuario: {}", user.getId());
    }

    @Override
    @Transactional
    public LogoutStatus revokeUserTokens(String accessToken) {
        try {
            Claims claims = jwtService.getClaimJWT(accessToken);
            String userId = claims.get("userID", String.class);

            if (userId == null) {
                logger.warn("No se pudo extraer el userId del token");
                return LogoutStatus.ERROR;
            }

            Long userIdLong = Long.parseLong(userId);
            logger.debug("Revocando tokens para usuario: {}", userIdLong);

            Optional<User> userOptional = userRepository.findById(userIdLong);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserAndRevokedFalse(user);

                if (activeTokens.isEmpty()) {
                    logger.info("Los tokens ya estaban revocados para usuario: {}", userIdLong);
                    return LogoutStatus.ALREADY_REVOKED;
                }

                sessionService.revokeAllUserTokens(userIdLong);
                logger.info("Tokens revocados exitosamente para usuario: {}", userIdLong);
                return LogoutStatus.SUCCESS;
            }

            logger.error("Usuario no encontrado: {}", userIdLong);
            return LogoutStatus.ERROR;
        } catch (Exception e) {
            logger.error("Error al revocar tokens: ", e);
            return LogoutStatus.ERROR;
        }
    }

    @Override
    public String getActiveRefreshToken(User user) {
        logger.debug("Buscando refresh token activo para usuario: {}", user.getId());
        
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserAndRevokedFalse(user);
        
        if (!activeTokens.isEmpty()) {
            String token = activeTokens.get(0).getRefreshToken();
            logger.debug("Refresh token activo encontrado para usuario: {}", user.getId());
            return token;
        }
        
        logger.debug("No se encontró refresh token activo para usuario: {}", user.getId());
        return null;
    }

    @Override
    public String extractUserId(String token) {
        try {
            return jwtService.extractUserId(token);
        } catch (Exception e) {
            logger.error("Error al extraer userId del token: ", e);
            return null;
        }
    }

    /**
     * Verifica si existe un token activo para el usuario
     * 
     * @param userId ID del usuario
     * @return true si existe un token activo, false en caso contrario
     */
    public boolean hasActiveToken(Long userId) {
        return refreshTokenRepository.existsByUser_IdAndRevokedFalse(userId);
    }
}
