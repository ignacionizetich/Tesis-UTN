package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.DTO.AuthDTO.LoginRequest;
import com.EDJ.ArCash.DTO.AuthDTO.LoginResponse;
import com.EDJ.ArCash.Models.*;
import com.EDJ.ArCash.Repository.*;
import com.EDJ.ArCash.Service.strategy.AuthenticationStrategy;
import com.EDJ.ArCash.Service.strategy.PasswordRecoveryStrategy;
import com.EDJ.ArCash.Service.strategy.TokenManagementStrategy;
import com.EDJ.ArCash.observer.Event;
import com.EDJ.ArCash.observer.EventPublisher;
import com.EDJ.ArCash.observer.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.EDJ.ArCash.Models.Imp.LogoutStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Servicio de autenticación refactorizado aplicando el Patrón Strategy y el Principio SRP
 * Este servicio ahora actúa como fachada, delegando las responsabilidades específicas
 * a servicios especializados:
 * - AuthenticationStrategy: Autenticación de usuarios
 * - TokenManagementStrategy: Gestión de tokens JWT
 * - PasswordRecoveryStrategy: Recuperación de contraseñas
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    @Qualifier("userAuthenticationService")
    private AuthenticationStrategy authenticationStrategy;

    @Autowired
    @Qualifier("jwtTokenManagementService")
    private TokenManagementStrategy tokenManagementStrategy;

    @Autowired
    @Qualifier("emailPasswordRecoveryService")
    private PasswordRecoveryStrategy passwordRecoveryStrategy;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private EventPublisher eventPublisher;

    /**
     * Autentica un usuario utilizando la estrategia de autenticación configurada
     * 
     * @param loginRequest Solicitud de login
     * @return LoginResponse con el resultado
     */
    public LoginResponse login(LoginRequest loginRequest) {
        logger.info("Procesando login para usuario: {}", loginRequest.getUsername());
        return authenticationStrategy.authenticate(loginRequest);
    }

    /**
     * Cierra la sesión del usuario revocando sus tokens
     * 
     * @param accessToken Token de acceso del usuario
     * @return Estado del logout
     */
    public LogoutStatus logout(String accessToken) {
        logger.info("Procesando logout");
        return tokenManagementStrategy.revokeUserTokens(accessToken);
    }

    /**
     * Valida si la sesión del usuario es válida
     * 
     * @param token Token de acceso
     * @return true si la sesión es válida, false en caso contrario
     */
    public boolean isValidSession(String token) {
        return authenticationStrategy.isValidSession(token);
    }

    /**
     * Envía un correo de recuperación de contraseña
     * 
     * @param email Email del usuario
     * @return true si se envió correctamente, false en caso contrario
     */
    @Transactional
    public boolean enviarCorreoRecuperacion(String email) {
        logger.info("Enviando correo de recuperación para: {}", email);
        return passwordRecoveryStrategy.sendRecoveryEmail(email);
    }

    /**
     * Cambia el alias y username del usuario
     * Esta funcionalidad permanece aquí ya que involucra lógica de negocio
     * relacionada con múltiples entidades (User, Account, Credentials)
     * 
     * @param userId ID del usuario
     * @param nuevoAlias Nuevo alias
     * @return true si el cambio fue exitoso, false en caso contrario
     */
    @Transactional
    public boolean cambiarAliasYUsername(Long userId, String nuevoAlias) {
        logger.info("Cambiando alias para usuario: {}", userId);
        
        String regex = "^(?=.*[A-Za-z])[A-Za-z\\d]{4,25}$";
        if (nuevoAlias == null || nuevoAlias.trim().isEmpty() ||
                !nuevoAlias.matches(regex) ||
                nuevoAlias.matches("^\\d+$")) {
            logger.warn("Formato de alias inválido para usuario: {}", userId);
            return false;
        }

        Optional<Account> accountOpt = accountRepository.findByUser_Id(userId);
        if (accountOpt.isEmpty()) {
            logger.warn("Cuenta no encontrada para usuario: {}", userId);
            return false;
        }
        
        Credentials credentials = accountOpt.get().getUser().getCredentials();
        User user = accountOpt.get().getUser();

        if (credentialRepository.findByUsername(nuevoAlias).isPresent()) {
            logger.warn("El alias ya está en uso: {}", nuevoAlias);
            return false;
        }

        String oldAlias = user.getAlias();
        user.setAlias(nuevoAlias);
        userRepository.saveAndFlush(user);

        credentials.setUsername(nuevoAlias);
        credentialRepository.save(credentials);
        
        // Publicar evento de cambio de alias
        Event event = new Event(EventType.ALIAS_CHANGED);
        event.addData("user", user);
        event.addData("oldAlias", oldAlias);
        event.addData("newAlias", nuevoAlias);
        eventPublisher.publish(event);
        
        logger.info("Alias cambiado exitosamente para usuario: {}", userId);
        return true;
    }

    /**
     * Valida si un token de recuperación es válido
     * 
     * @param tokenValue Valor del token
     * @return true si el token es válido, false en caso contrario
     */
    public boolean tokenValido(String tokenValue) {
        return passwordRecoveryStrategy.validateRecoveryToken(tokenValue);
    }

    /**
     * Reenvía el enlace de recuperación de contraseña
     * 
     * @param email Email del usuario
     * @return true si se envió exitosamente, false si no se pudo enviar
     */
    public boolean resendPasswordRecovery(String email) {
        logger.info("Reenviando recuperación de contraseña para: {}", email);
        return passwordRecoveryStrategy.resendRecoveryLink(email);
    }
}