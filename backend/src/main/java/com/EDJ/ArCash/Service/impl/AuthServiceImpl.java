package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.RefreshTokenCleanupService;
import com.EDJ.ArCash.Service.interfaces.AccountService;
import com.EDJ.ArCash.Service.interfaces.AuthService;
import com.EDJ.ArCash.Service.result.*;

import com.EDJ.ArCash.DTO.AuthDTO.LoginRequest;
import com.EDJ.ArCash.DTO.AuthDTO.LoginResponse;
import com.EDJ.ArCash.Models.*;
import com.EDJ.ArCash.Repository.*;
import com.EDJ.ArCash.Service.strategy.AuthenticationResult;
import com.EDJ.ArCash.Service.strategy.AuthenticationStrategy;
import com.EDJ.ArCash.Service.strategy.PasswordRecoveryStrategy;
import com.EDJ.ArCash.Service.strategy.TokenManagementStrategy;
import com.EDJ.ArCash.factory.LoginResponseFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.EDJ.ArCash.Models.Imp.LogoutStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);


    @Qualifier("userAuthenticationService")
    private final AuthenticationStrategy authenticationStrategy;


    @Qualifier("jwtTokenManagementService")
    private final TokenManagementStrategy tokenManagementStrategy;


    @Qualifier("emailPasswordRecoveryService")
    private final PasswordRecoveryStrategy passwordRecoveryStrategy;


    private final LoginResponseFactory loginResponseFactory;


    private final AccountRepository accountRepository;


    private final AccountService accountService;


    private final RefreshTokenCleanupService refreshTokenCleanupService;

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

    public LogoutStatus logout(String accessToken) {
        logger.info("Procesando logout");
        return tokenManagementStrategy.revokeUserTokens(accessToken);
    }

    public boolean isValidSession(String token) {
        return tokenManagementStrategy.isValidSession(token);
    }

    @Transactional
    public boolean enviarCorreoRecuperacion(String email) {
        logger.info("Enviando correo de recuperación para: {}", email);
        return passwordRecoveryStrategy.sendRecoveryEmail(email);
    }

    public boolean tokenValido(String tokenValue) {
        return passwordRecoveryStrategy.validateRecoveryToken(tokenValue);
    }

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
