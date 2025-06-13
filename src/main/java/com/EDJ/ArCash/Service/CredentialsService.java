package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.RecoveryToken;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.CredentialRepository;
import com.EDJ.ArCash.Repository.RecoveryTokenRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CredentialsService {

    
    private final RecoveryTokenRepository recoveryTokenRepository;
    private final CredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CredentialsService(CredentialRepository credentialRepository, PasswordEncoder passwordEncoder, RecoveryTokenRepository recoveryTokenRepository, UserRepository userRepository) {
        this.credentialRepository = credentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.recoveryTokenRepository = recoveryTokenRepository;
        this.userRepository = userRepository;
    }

    public void createCredentials(User user, String rawPassword){
        Credentials credentials = new Credentials();
        credentials.setUsername(user.getAlias());
        credentials.setPass(passwordEncoder.encode(rawPassword));
        credentials.setUser(user);
        credentialRepository.save(credentials);
    }

    @Transactional
    public String actualizarPassword(String tokenValue, String nuevaPassword, String confirmarPassword) {
        if (!nuevaPassword.equals(confirmarPassword)) {
            return "Las contraseñas no coinciden.";
        }

        Optional<RecoveryToken> recoveryToken = recoveryTokenRepository.findByToken(tokenValue);

        if (recoveryToken.isEmpty()) {
            return "Token no válido.";
        }

        RecoveryToken token = recoveryToken.get();

        if (token.isUsed()) {
            return "El token ya ha sido usado.";
        }

        if (token.getExpirationDate().isBefore(LocalDateTime.now())) {
            return "El token ha expirado.";
        }

        User user = token.getUser();
        user.getCredentials().setPass(passwordEncoder.encode(nuevaPassword));
        userRepository.save(user);
        token.setUsed(true);
        recoveryTokenRepository.saveAndFlush(token);

        return "Contraseña actualizada correctamente.";
    }



}
