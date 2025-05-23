package com.EDJ.ArCash.Service;


import com.EDJ.ArCash.DTO.LoginRequest;
import com.EDJ.ArCash.DTO.LoginResponse;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.RefreshToken;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.CredentialRepository;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.Security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CredentialRepository credentialRepository;

    // Método para manejar el login
    public LoginResponse login(LoginRequest loginRequest) {
        Optional<Credentials> credentialsOptional = credentialRepository.findByUsername(loginRequest.getUsername());

        if (credentialsOptional.isPresent()) {
            Credentials credentials = credentialsOptional.get();
            User usuario = credentials.getUser();

            if (passwordEncoder.matches(loginRequest.getPassword(), credentials.getPass())) {
                if (!usuario.isEnabled()) {
                    return new LoginResponse(false, "Usuario no habilitado");
                }

                // Generar el access token
                String accessToken = jwtUtils.generateToken(String.valueOf(usuario.getId_user()));

                // Generar el refresh token y guardarlo en la base de datos
                String refreshToken = jwtUtils.generateRefreshToken(String.valueOf(usuario.getId_user()));
                saveRefreshToken(usuario, refreshToken);

                return new LoginResponse(true, "Login exitoso");
            } else {
                return new LoginResponse(false, "Credenciales incorrectas");
            }
        }

        return new LoginResponse(false, "Usuario no encontrado");
    }

    // Método para guardar el refresh token en la base de datos
    private void saveRefreshToken(User usuario, String refreshToken) {

        RefreshToken token = new RefreshToken();
        token.setUser(usuario);
        token.setRefreshToken(refreshToken);
        token.setIssuedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusDays(7)); // 7 días
        token.setRevoked(false);
        refreshTokenRepository.save(token);
    }


}

