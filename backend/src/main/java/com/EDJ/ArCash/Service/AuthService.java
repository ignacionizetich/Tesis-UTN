package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.DTO.AuthDTO.LoginRequest;
import com.EDJ.ArCash.DTO.AuthDTO.LoginResponse;
import com.EDJ.ArCash.Models.*;
import com.EDJ.ArCash.Repository.*;
import com.EDJ.ArCash.Service.strategy.AuthenticationResult;
import com.EDJ.ArCash.Service.strategy.AuthenticationStrategy;
import com.EDJ.ArCash.Service.strategy.PasswordRecoveryStrategy;
import com.EDJ.ArCash.Service.strategy.TokenManagementStrategy;
import com.EDJ.ArCash.factory.LoginResponseFactory;
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
 * Fachada de autenticacion:
 * - AuthenticationStrategy: valida credenciales
 * - TokenManagementStrategy: emite / revoca tokens y consulta sesion
 * - PasswordRecoveryStrategy: recuperacion de contraseñas
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
    private LoginResponseFactory loginResponseFactory;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private EventPublisher eventPublisher;

    /**
     * Valida credenciales, emite tokens y recien despues resuelve la cuenta ARS.
     * El orden (tokens antes del chequeo de cuenta) preserva el comportamiento historico
     * de UserAuthenticationService: sin cuenta ARS igual se generan/guardan tokens
     * y la respuesta es error "Cuenta no encontrada".
     */
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        logger.info("Procesando login para usuario: {}", loginRequest.getUsername());

        AuthenticationResult resultado = authenticationStrategy.authenticate(loginRequest);
        if (!resultado.isSuccess()) {
            return loginResponseFactory.createErrorResponse(resultado.getErrorMessage());
        }

        User user = resultado.getUser();

        // Obtener o generar refresh token (antes del chequeo de cuenta: comportamiento actual)
        String refreshToken = tokenManagementStrategy.getActiveRefreshToken(user);
        if (refreshToken == null) {
            refreshToken = tokenManagementStrategy.generateRefreshToken(
                    String.valueOf(user.getId()),
                    user.getPermissions().name()
            );
            tokenManagementStrategy.saveRefreshToken(user, refreshToken);
        }

        String accessToken = tokenManagementStrategy.generateAccessToken(
                String.valueOf(user.getId()),
                user.getPermissions().name()
        );

        Optional<Account> optionalAccount = accountRepository.findByUser_Id(user.getId());
        if (optionalAccount.isEmpty()) {
            logger.error("Cuenta no encontrada para usuario: {}", user.getId());
            return loginResponseFactory.createErrorResponse("Cuenta no encontrada");
        }

        logger.info("Usuario autenticado exitosamente: {}", loginRequest.getUsername());
        return loginResponseFactory.createSuccessResponse(
                accessToken,
                refreshToken,
                optionalAccount.get().getIdAccount(),
                user.getPermissions().name()
        );
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
        return tokenManagementStrategy.isValidSession(token);
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