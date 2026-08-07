package com.EDJ.ArCash.Service.strategy;

import com.EDJ.ArCash.Models.RecoveryToken;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.RecoveryTokenRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.Service.RecoveryTokenService;
import com.EDJ.ArCash.observer.Event;
import com.EDJ.ArCash.observer.EventPublisher;
import com.EDJ.ArCash.observer.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementación de PasswordRecoveryStrategy para recuperación de contraseñas por email
 * Responsable únicamente de la gestión de recuperación de contraseñas
 */
@Service("emailPasswordRecoveryService")
public class EmailPasswordRecoveryService implements PasswordRecoveryStrategy {

    private static final Logger logger = LoggerFactory.getLogger(EmailPasswordRecoveryService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecoveryTokenRepository recoveryTokenRepository;

    @Autowired
    private RecoveryTokenService recoveryTokenService;

    @Autowired
    private EventPublisher eventPublisher;

    @Override
    @Transactional
    public boolean sendRecoveryEmail(String email) {
        logger.info("Solicitando recuperación de contraseña para email: {}", email);

        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            logger.warn("Usuario no encontrado para email: {}", email);
            return false;
        }

        User user = userOpt.get();
        
        try {
            // Crear o actualizar el token de recuperación
            String token = recoveryTokenService.createRecoveryToken(user);
            
            // Publicar evento para envío de email
            Event event = new Event(EventType.PASSWORD_RECOVERY_REQUESTED);
            event.addData("user", user);
            event.addData("token", token);
            eventPublisher.publish(event);
            
            logger.info("Email de recuperación enviado exitosamente para: {}", email);
            return true;
            
        } catch (Exception e) {
            logger.error("Error al enviar email de recuperación para {}: ", email, e);
            return false;
        }
    }

    @Override
    public boolean validateRecoveryToken(String tokenValue) {
        logger.debug("Validando token de recuperación");

        Optional<RecoveryToken> optionalToken = recoveryTokenRepository.findByToken(tokenValue);
        
        if (optionalToken.isEmpty()) {
            logger.warn("Token de recuperación no encontrado");
            return false;
        }

        RecoveryToken token = optionalToken.get();
        
        // Validar que no esté usado y no esté expirado
        boolean isValid = !token.isUsed() && token.getExpirationDate().isAfter(LocalDateTime.now());
        
        if (!isValid) {
            logger.warn("Token de recuperación inválido o expirado");
        } else {
            logger.debug("Token de recuperación válido");
        }
        
        return isValid;
    }

    @Override
    public boolean resendRecoveryLink(String email) {
        logger.info("Reenviando link de recuperación para email: {}", email);
        
        try {
            boolean result = sendRecoveryEmail(email);
            
            if (result) {
                logger.info("Link de recuperación reenviado exitosamente para: {}", email);
            } else {
                logger.warn("No se pudo reenviar link de recuperación para: {}", email);
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error al reenviar link de recuperación para {}: ", email, e);
            return false;
        }
    }
}
