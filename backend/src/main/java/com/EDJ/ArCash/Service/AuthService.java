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
    private AccountRepository accountRepository;

    @Autowired
    private RefreshTokenCleanupService refreshTokenCleanupService;

    /**
     * Valida credenciales, exige cuenta ARS y recien entonces emite tokens.
     * Sin cuenta ARS la respuesta sigue siendo error "Cuenta no encontrada",
     * pero no se genera ni persiste ningun refresh/access token.
     */
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        logger.info("Procesando login para usuario: {}", loginRequest.getUsername());

        AuthenticationResult resultado = authenticationStrategy.authenticate(loginRequest);
        if (!resultado.isSuccess()) {
            return loginResponseFactory.createErrorResponse(resultado.getErrorMessage());
        }

        User user = resultado.getUser();

        Optional<Account> optionalAccount = accountRepository.findByUser_Id(user.getId());
        if (optionalAccount.isEmpty()) {
            logger.error("Cuenta no encontrada para usuario: {}", user.getId());
            return loginResponseFactory.createErrorResponse("Cuenta no encontrada");
        }

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

    /**
     * Emite un nuevo access token a partir de un refresh token (cookie).
     */
    public RefreshAccessResult refreshAccessToken(String refreshToken) {
        if (refreshToken == null) {
            return RefreshAccessResult.missing();
        }

        Optional<RefreshToken> tokenOpt = refreshTokenCleanupService.getRefreshTokenAndRevokedFalse(refreshToken);
        if (tokenOpt.isEmpty() || tokenOpt.get().getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            return RefreshAccessResult.invalid();
        }

        User user = tokenOpt.get().getUser();
        String newAccessToken = tokenManagementStrategy.generateAccessToken(
                String.valueOf(user.getId()),
                user.getPermissions().name()
        );
        return RefreshAccessResult.ok(newAccessToken);
    }
}