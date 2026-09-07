package com.EDJ.ArCash.Service.strategy;

import com.EDJ.ArCash.DTO.AuthDTO.LoginRequest;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.CredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("userAuthenticationService")
public class UserAuthenticationService implements AuthenticationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(UserAuthenticationService.class);
    private static final String STRATEGY_TYPE = "USER_CREDENTIALS";

    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAuthenticationService(CredentialRepository credentialRepository,
                                     PasswordEncoder passwordEncoder) {
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AuthenticationResult authenticate(LoginRequest loginRequest) {
        logger.info("Intentando autenticar usuario: {}", loginRequest.getUsername());

        Optional<Credentials> credentialsOptional = credentialRepository.findByUsername(loginRequest.getUsername());

        if (credentialsOptional.isEmpty()) {
            logger.warn("Usuario no encontrado: {}", loginRequest.getUsername());
            return AuthenticationResult.failure("Usuario no encontrado");
        }

        Credentials credentials = credentialsOptional.get();
        User user = credentials.getUser();


        if (!passwordEncoder.matches(loginRequest.getPassword(), credentials.getPass())) {
            logger.warn("Credenciales incorrectas para usuario: {}", loginRequest.getUsername());
            return AuthenticationResult.failure("Credenciales incorrectas");
        }

      if (!user.isActive()) {
        logger.warn("Usuario no habilitado: {}", loginRequest.getUsername());
        return AuthenticationResult.failure(AuthenticationResult.USER_DISABLED_MESSAGE);
      }

        logger.info("Credenciales validas para usuario: {}", loginRequest.getUsername());
        return AuthenticationResult.success(user);
    }

    @Override
    public String getStrategyType() {
        return STRATEGY_TYPE;
    }
}
