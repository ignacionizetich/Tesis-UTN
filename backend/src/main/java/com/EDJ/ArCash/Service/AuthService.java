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

import java.util.List;
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
    private AccountService accountService;

    @Autowired
    private RefreshTokenCleanupService refreshTokenCleanupService;

    /**
     * Valida credenciales, exige cuenta ARS y recien entonces emite tokens.
     * Si el usuario esta activo pero nunca tuvo cuenta ARS (p. ej. enable por admin),
     * se crea en el momento.
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
            List<Account> existentes = accountRepository.findAllByUser_Id(user.getId());
            if (existentes.isEmpty()) {
                logger.warn("Usuario {} activo sin cuentas; creando cuenta ARS automaticamente", user.getId());
                optionalAccount = Optional.of(accountService.ensureArsAccount(user));
            } else {
                logger.error("Cuenta ARS no encontrada para usuario: {} (tiene {} cuenta(s) de otro tipo)",
                        user.getId(), existentes.size());
                return loginResponseFactory.createErrorResponse("Cuenta no encontrada");
            }
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

    /**
     * Interpreta Authorization Bearer y consulta sesion vigente.
     */
    public SessionCheckResult checkSession(String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (isValidSession(token)) {
                    return SessionCheckResult.active();
                }
            }
            return SessionCheckResult.inactive();
        } catch (Exception e) {
            return SessionCheckResult.error();
        }
    }

    public RecoverMailResult sendRecoverMail(String email) {
        try {
            boolean enviado = enviarCorreoRecuperacion(email);
            return enviado ? RecoverMailResult.ok() : RecoverMailResult.notFound();
        } catch (Exception e) {
            return RecoverMailResult.error();
        }
    }

    public RecoveryTokenValidationResult validateRecoveryToken(String token) {
        try {
            return tokenValido(token)
                    ? RecoveryTokenValidationResult.valid()
                    : RecoveryTokenValidationResult.invalid();
        } catch (Exception e) {
            return RecoveryTokenValidationResult.error();
        }
    }

    public ResendEmailResult resendPasswordRecoveryEmail(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResendEmailResult.emailRequired();
            }
            resendPasswordRecovery(email.trim());
            return ResendEmailResult.ok(
                    "Si el email está registrado, te enviamos un enlace de recuperación.");
        } catch (Exception e) {
            return ResendEmailResult.error();
        }
    }
}
