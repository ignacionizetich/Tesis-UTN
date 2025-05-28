package com.EDJ.ArCash.Service;


import com.EDJ.ArCash.DTO.LoginRequest;
import com.EDJ.ArCash.DTO.LoginResponse;
import com.EDJ.ArCash.Models.Account;
import com.EDJ.ArCash.Models.Credentials;
import com.EDJ.ArCash.Models.RefreshToken;
import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Repository.AccountRepository;
import com.EDJ.ArCash.Repository.CredentialRepository;
import com.EDJ.ArCash.Repository.RefreshTokenRepository;
import com.EDJ.ArCash.Repository.UserRepository;
import com.EDJ.ArCash.Security.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.EDJ.ArCash.Models.Imp.LogoutStatus;
import java.time.LocalDateTime;
import java.util.List;
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

    @Autowired
    private AccountRepository accountRepository;

    // Método para manejar el login

    public LoginResponse login(LoginRequest loginRequest) {
        Optional<Credentials> credentialsOptional = credentialRepository.findByUsername(loginRequest.getUsername());

        if (credentialsOptional.isPresent()) {
            Credentials credentials = credentialsOptional.get();
            User usuario = credentials.getUser();

            // Verificar si ya tiene una sesión activa usando List en lugar de Optional
            List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserAndRevokedFalse(usuario);
            if (!activeTokens.isEmpty()) {
                return new LoginResponse(false, "Ya existe una sesión activa para este usuario", null, null, null);
            }

            if (passwordEncoder.matches(loginRequest.getPassword(), credentials.getPass())) {
                if (!usuario.isEnabled()) {
                    return new LoginResponse(false, "Usuario no habilitado", null, null, null);
                }

                // Generar el access token
                String accessToken = jwtUtils.generateToken(String.valueOf(usuario.getIduser()));

                // Generar el refresh token y guardarlo en la base de datos
                String refreshToken = jwtUtils.generateRefreshToken(String.valueOf(usuario.getIduser()));
                saveRefreshToken(usuario, refreshToken);

                Optional<Account> optionalAccount = accountRepository.findByUserIduser(usuario.getIduser());
                if (optionalAccount.isPresent()) {
                    Account account = optionalAccount.get();
                    return new LoginResponse(true, "Login exitoso", accessToken, refreshToken, account.getIdAccount());
                }
                return new LoginResponse(false, "Cuenta no encontrada", null, null, null);
            }
            return new LoginResponse(false, "Credenciales incorrectas", null, null, null);
        }
        return new LoginResponse(false, "Usuario no encontrado", null, null, null);
    }



    public LogoutStatus logout(String accessToken) {
        try {
            // Intentar extraer el userId del token sin validar su expiración
            Claims claims = JwtUtils.getClaimJWT(accessToken);
            String userId = claims.get("userID", String.class);

            if (userId == null) {
                return LogoutStatus.ERROR;
            }

            Long userIdLong = Long.parseLong(userId);

            // Buscar y revocar todos los refresh tokens activos
            Optional<User> userOptional = userRepository.findById(userIdLong);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserAndRevokedFalse(user);

                if (activeTokens.isEmpty()) {
                    return LogoutStatus.ALREADY_REVOKED;
                }

                // Revocar todos los tokens activos
                jwtUtils.revokeAllUserTokens(userIdLong);
                return LogoutStatus.SUCCESS;
            }

            return LogoutStatus.ERROR;
        } catch (Exception e) {
            e.printStackTrace();
            return LogoutStatus.ERROR;
        }
    }



    public boolean isValidSession(String token) {
        try {
            String userId = jwtUtils.extractUserId(token);
            if (userId == null) return false;

            // Verificar si existe un refresh token activo para el usuario
            return refreshTokenRepository
                    .existsByUser_IduserAndRevokedFalse(Long.parseLong(userId));

        } catch (Exception e) {
            return false;
        }
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