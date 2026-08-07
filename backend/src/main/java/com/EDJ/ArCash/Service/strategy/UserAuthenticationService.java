package com.EDJ.ArCash.Service.strategy;

import com.EDJ.ArCash.DTO.AuthDTO.LoginRequest;
import com.EDJ.ArCash.DTO.AuthDTO.LoginResponse;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.CredentialRepository;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import com.EDJ.ArCash.factory.LoginResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementación de AuthenticationStrategy para autenticación de usuarios
 * Responsable únicamente de autenticar usuarios con credenciales
 */
@Service("userAuthenticationService")
public class UserAuthenticationService implements AuthenticationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(UserAuthenticationService.class);
    private static final String STRATEGY_TYPE = "USER_CREDENTIALS";

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginResponseFactory loginResponseFactory;

    @Autowired
    private TokenManagementStrategy tokenManagementStrategy;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public LoginResponse authenticate(LoginRequest loginRequest) {
        logger.info("Intentando autenticar usuario: {}", loginRequest.getUsername());

        Optional<Credentials> credentialsOptional = credentialRepository.findByUsername(loginRequest.getUsername());

        if (credentialsOptional.isEmpty()) {
            logger.warn("Usuario no encontrado: {}", loginRequest.getUsername());
            return loginResponseFactory.createErrorResponse("Usuario no encontrado");
        }

        Credentials credentials = credentialsOptional.get();
        User user = credentials.getUser();

        // Validar contraseña
        if (!passwordEncoder.matches(loginRequest.getPassword(), credentials.getPass())) {
            logger.warn("Credenciales incorrectas para usuario: {}", loginRequest.getUsername());
            return loginResponseFactory.createErrorResponse("Credenciales incorrectas");
        }

        // Validar que el usuario esté activo
        if (!user.isActive()) {
            logger.warn("Usuario no habilitado: {}", loginRequest.getUsername());
            return loginResponseFactory.createErrorResponse("Usuario no habilitado");
        }

        // Obtener o generar refresh token
        String refreshToken = tokenManagementStrategy.getActiveRefreshToken(user);
        if (refreshToken == null) {
            refreshToken = tokenManagementStrategy.generateRefreshToken(
                    String.valueOf(user.getId()),
                    user.getPermissions().name()
            );
            tokenManagementStrategy.saveRefreshToken(user, refreshToken);
        }

        // Generar access token
        String accessToken = tokenManagementStrategy.generateAccessToken(
                String.valueOf(user.getId()),
                user.getPermissions().name()
        );

        // Obtener información de la cuenta
        Optional<Account> optionalAccount = accountRepository.findByUser_Id(user.getId());
        if (optionalAccount.isEmpty()) {
            logger.error("Cuenta no encontrada para usuario: {}", user.getId());
            return loginResponseFactory.createErrorResponse("Cuenta no encontrada");
        }

        Account account = optionalAccount.get();
        logger.info("Usuario autenticado exitosamente: {}", loginRequest.getUsername());
        
        return loginResponseFactory.createSuccessResponse(
                accessToken,
                refreshToken,
                account.getIdAccount(),
                user.getPermissions().name()
        );
    }

    @Override
    public boolean isValidSession(String token) {
        try {
            String userId = tokenManagementStrategy.extractUserId(token);
            if (userId == null) {
                logger.debug("No se pudo extraer userId del token");
                return false;
            }

            boolean isValid = refreshTokenRepository.existsByUser_IdAndRevokedFalse(Long.parseLong(userId));
            logger.debug("Validación de sesión para usuario {}: {}", userId, isValid);
            
            return isValid;

        } catch (Exception e) {
            logger.error("Error al validar sesión: ", e);
            return false;
        }
    }

    @Override
    public String getStrategyType() {
        return STRATEGY_TYPE;
    }
}
