# Guía de Uso - AuthService Refactorizado

## 📖 Índice
1. [Uso Básico](#uso-básico)
2. [Inyección de Dependencias](#inyección-de-dependencias)
3. [Extensión con Nuevas Strategies](#extensión-con-nuevas-strategies)
4. [Testing](#testing)
5. [Ejemplos Avanzados](#ejemplos-avanzados)

---

## 🚀 Uso Básico

### En Controladores (No requiere cambios)

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // El AuthService delega internamente a UserAuthenticationService
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        // El AuthService delega internamente a JwtTokenManagementService
        LogoutStatus status = authService.logout(token);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/recover")
    public ResponseEntity<?> recoverPassword(@RequestBody String email) {
        // El AuthService delega internamente a EmailPasswordRecoveryService
        boolean sent = authService.enviarCorreoRecuperacion(email);
        return ResponseEntity.ok(sent);
    }
}
```

---

## 🔌 Inyección de Dependencias

### Usando AuthService como Fachada (Recomendado)

```java
@Service
public class MiServicio {

    @Autowired
    private AuthService authService;

    public void miMetodo() {
        // Interfaz simple y unificada
        authService.login(request);
        authService.logout(token);
    }
}
```

### Usando Strategies Directamente (Para casos especiales)

```java
@Service
public class MiServicioAvanzado {

    // Inyección por interfaz
    @Autowired
    @Qualifier("userAuthenticationService")
    private AuthenticationStrategy authStrategy;

    @Autowired
    @Qualifier("jwtTokenManagementService")
    private TokenManagementStrategy tokenStrategy;

    public void autenticarYGenerarToken(LoginRequest request) {
        // Control granular sobre cada paso
        LoginResponse response = authStrategy.authenticate(request);
        
        if (response.isSuccess()) {
            String token = tokenStrategy.generateAccessToken(
                userId, 
                role
            );
            // ... lógica adicional
        }
    }
}
```

---

## 🔧 Extensión con Nuevas Strategies

### Ejemplo 1: Implementar OAuth Authentication

```java
package com.EDJ.ArCash.Service.strategy;

import org.springframework.stereotype.Service;

/**
 * Nueva estrategia de autenticación usando OAuth
 */
@Service("oAuthAuthenticationService")
public class OAuthAuthenticationService implements AuthenticationStrategy {

    @Autowired
    private OAuthClientProvider oAuthProvider;

    @Autowired
    private TokenManagementStrategy tokenManagement;

    @Override
    public LoginResponse authenticate(LoginRequest loginRequest) {
        // 1. Validar OAuth token con el proveedor
        OAuthUser oAuthUser = oAuthProvider.validateToken(
            loginRequest.getOAuthToken()
        );

        if (oAuthUser == null) {
            return new LoginResponse(false, "OAuth token inválido");
        }

        // 2. Buscar o crear usuario en nuestra DB
        User user = findOrCreateUser(oAuthUser);

        // 3. Generar nuestros tokens JWT
        String accessToken = tokenManagement.generateAccessToken(
            String.valueOf(user.getId()),
            user.getPermissions().name()
        );

        String refreshToken = tokenManagement.getActiveRefreshToken(user);
        if (refreshToken == null) {
            refreshToken = tokenManagement.generateRefreshToken(
                String.valueOf(user.getId()),
                user.getPermissions().name()
            );
            tokenManagement.saveRefreshToken(user, refreshToken);
        }

        return new LoginResponse(
            true, 
            "Login exitoso", 
            accessToken, 
            refreshToken
        );
    }

    @Override
    public boolean isValidSession(String token) {
        // Validar sesión OAuth
        return oAuthProvider.isValidSession(token);
    }

    @Override
    public String getStrategyType() {
        return "OAUTH";
    }

    private User findOrCreateUser(OAuthUser oAuthUser) {
        // Lógica para buscar o crear usuario
        // ...
    }
}
```

#### Configurar para usar OAuth

```java
@Service
public class AuthService {

    // Cambiar el qualifier para usar OAuth
    @Autowired
    @Qualifier("oAuthAuthenticationService")  // ← Cambio aquí
    private AuthenticationStrategy authenticationStrategy;

    // ... resto del código igual
}
```

### Ejemplo 2: Implementar 2FA (Two-Factor Authentication)

```java
package com.EDJ.ArCash.Service.strategy;

import org.springframework.stereotype.Service;

/**
 * Estrategia de autenticación con 2FA
 */
@Service("twoFactorAuthenticationService")
public class TwoFactorAuthenticationService implements AuthenticationStrategy {

    @Autowired
    private UserAuthenticationService baseAuthService;

    @Autowired
    private TwoFactorProvider twoFactorProvider;

    @Autowired
    private TokenManagementStrategy tokenManagement;

    @Override
    public LoginResponse authenticate(LoginRequest loginRequest) {
        // 1. Autenticación básica primero
        LoginResponse baseResponse = baseAuthService.authenticate(loginRequest);

        if (!baseResponse.isSuccess()) {
            return baseResponse;
        }

        // 2. Verificar código 2FA
        if (!loginRequest.hasTwoFactorCode()) {
            // Enviar código 2FA
            twoFactorProvider.sendCode(loginRequest.getUsername());
            return new LoginResponse(
                false, 
                "Se requiere código 2FA. Revisa tu email/SMS"
            );
        }

        // 3. Validar código 2FA
        boolean isValid2FA = twoFactorProvider.validateCode(
            loginRequest.getUsername(),
            loginRequest.getTwoFactorCode()
        );

        if (!isValid2FA) {
            return new LoginResponse(false, "Código 2FA inválido");
        }

        // 4. Login exitoso, retornar tokens
        return baseResponse;
    }

    @Override
    public boolean isValidSession(String token) {
        return baseAuthService.isValidSession(token);
    }

    @Override
    public String getStrategyType() {
        return "TWO_FACTOR";
    }
}
```

### Ejemplo 3: Implementar Recuperación por SMS

```java
package com.EDJ.ArCash.Service.strategy;

import org.springframework.stereotype.Service;

/**
 * Estrategia de recuperación de contraseña por SMS
 */
@Service("smsPasswordRecoveryService")
public class SmsPasswordRecoveryService implements PasswordRecoveryStrategy {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecoveryTokenService recoveryTokenService;

    @Autowired
    private SmsService smsService;

    @Override
    public boolean sendRecoveryEmail(String phoneNumber) {
        Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        String token = recoveryTokenService.createRecoveryToken(user);

        // Enviar SMS en lugar de email
        String message = String.format(
            "Tu código de recuperación es: %s. Válido por 15 minutos.",
            token.substring(0, 6) // Solo primeros 6 caracteres
        );

        return smsService.sendSms(phoneNumber, message);
    }

    @Override
    public boolean validateRecoveryToken(String token) {
        return recoveryTokenService.isValid(token);
    }

    @Override
    public boolean resendRecoveryLink(String phoneNumber) {
        return sendRecoveryEmail(phoneNumber);
    }
}
```

---

## 🧪 Testing

### Test de UserAuthenticationService

```java
package com.EDJ.ArCash.Service.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthenticationServiceTest {

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenManagementStrategy tokenManagementStrategy;

    @Mock
    private LoginResponseFactory loginResponseFactory;

    @InjectMocks
    private UserAuthenticationService authService;

    @Test
    void authenticate_ConCredencialesValidas_DebeRetornarExito() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");
        
        Credentials credentials = new Credentials();
        credentials.setPass("hashedPassword");
        
        User user = new User();
        user.setId(1L);
        user.setActive(true);
        user.setPermissions(Permissions.USER);
        credentials.setUser(user);

        Account account = new Account();
        account.setIdAccount(100L);

        when(credentialRepository.findByUsername("testuser"))
            .thenReturn(Optional.of(credentials));
        when(passwordEncoder.matches("password123", "hashedPassword"))
            .thenReturn(true);
        when(accountRepository.findByUser_Id(1L))
            .thenReturn(Optional.of(account));
        when(tokenManagementStrategy.getActiveRefreshToken(user))
            .thenReturn("refresh-token");
        when(tokenManagementStrategy.generateAccessToken("1", "USER"))
            .thenReturn("access-token");
        when(loginResponseFactory.createSuccessResponse(
            anyString(), anyString(), anyLong(), anyString()))
            .thenReturn(new LoginResponse(true, "Login exitoso"));

        // Act
        LoginResponse response = authService.authenticate(request);

        // Assert
        assertTrue(response.isSuccess());
        verify(credentialRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("password123", "hashedPassword");
        verify(tokenManagementStrategy).generateAccessToken("1", "USER");
    }

    @Test
    void authenticate_ConCredencialesInvalidas_DebeRetornarError() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");
        
        Credentials credentials = new Credentials();
        credentials.setPass("hashedPassword");
        User user = new User();
        credentials.setUser(user);

        when(credentialRepository.findByUsername("testuser"))
            .thenReturn(Optional.of(credentials));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword"))
            .thenReturn(false);
        when(loginResponseFactory.createErrorResponse("Credenciales incorrectas"))
            .thenReturn(new LoginResponse(false, "Credenciales incorrectas"));

        // Act
        LoginResponse response = authService.authenticate(request);

        // Assert
        assertFalse(response.isSuccess());
        verify(passwordEncoder).matches("wrongpassword", "hashedPassword");
        verify(tokenManagementStrategy, never()).generateAccessToken(anyString(), anyString());
    }

    @Test
    void isValidSession_ConTokenValido_DebeRetornarTrue() {
        // Arrange
        String token = "valid-token";
        when(tokenManagementStrategy.extractUserId(token)).thenReturn("1");
        when(refreshTokenRepository.existsByUser_IdAndRevokedFalse(1L)).thenReturn(true);

        // Act
        boolean result = authService.isValidSession(token);

        // Assert
        assertTrue(result);
    }
}
```

### Test de JwtTokenManagementService

```java
@ExtendWith(MockitoExtension.class)
class JwtTokenManagementServiceTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtTokenManagementService tokenService;

    @Test
    void generateAccessToken_DebeGenerarTokenCorrectamente() {
        // Arrange
        when(jwtUtils.generateToken("1", "USER")).thenReturn("access-token");

        // Act
        String token = tokenService.generateAccessToken("1", "USER");

        // Assert
        assertEquals("access-token", token);
        verify(jwtUtils).generateToken("1", "USER");
    }

    @Test
    void saveRefreshToken_DebeGuardarTokenEnBD() {
        // Arrange
        User user = new User();
        user.setId(1L);
        String refreshToken = "refresh-token";

        when(refreshTokenRepository.save(any(RefreshToken.class)))
            .thenAnswer(i -> i.getArgument(0));

        // Act
        tokenService.saveRefreshToken(user, refreshToken);

        // Assert
        verify(refreshTokenRepository).save(argThat(token ->
            token.getUser().equals(user) &&
            token.getRefreshToken().equals(refreshToken) &&
            !token.isRevoked()
        ));
    }

    @Test
    void revokeUserTokens_DebeRevocarTokensExitosamente() {
        // Arrange
        String accessToken = "access-token";
        Claims claims = mock(Claims.class);
        when(claims.get("userID", String.class)).thenReturn("1");
        when(jwtUtils.getClaimJWT(accessToken)).thenReturn(claims);

        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        RefreshToken refreshToken = new RefreshToken();
        when(refreshTokenRepository.findAllByUserAndRevokedFalse(user))
            .thenReturn(List.of(refreshToken));

        // Act
        LogoutStatus status = tokenService.revokeUserTokens(accessToken);

        // Assert
        assertEquals(LogoutStatus.SUCCESS, status);
        verify(jwtUtils).revokeAllUserTokens(1L);
    }
}
```

### Test de Integration

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Test
    @Transactional
    void login_flujoCompleto_debeAutenticarCorrectamente() {
        // Arrange
        User user = crearUsuarioDeTest();
        LoginRequest request = new LoginRequest(user.getAlias(), "password123");

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
    }

    private User crearUsuarioDeTest() {
        // Lógica para crear usuario de test
        // ...
    }
}
```

---

## 🎯 Ejemplos Avanzados

### Selector Dinámico de Strategy

```java
@Service
public class AuthenticationStrategySelector {

    private final Map<String, AuthenticationStrategy> strategies = new HashMap<>();

    @Autowired
    public AuthenticationStrategySelector(
            @Qualifier("userAuthenticationService") AuthenticationStrategy userAuth,
            @Qualifier("oAuthAuthenticationService") AuthenticationStrategy oAuth,
            @Qualifier("twoFactorAuthenticationService") AuthenticationStrategy twoFactor) {
        
        strategies.put("USER_CREDENTIALS", userAuth);
        strategies.put("OAUTH", oAuth);
        strategies.put("TWO_FACTOR", twoFactor);
    }

    public AuthenticationStrategy getStrategy(String type) {
        AuthenticationStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Estrategia no soportada: " + type);
        }
        return strategy;
    }
}

// Uso
@Service
public class FlexibleAuthService {

    @Autowired
    private AuthenticationStrategySelector strategySelector;

    public LoginResponse login(LoginRequest request, String authType) {
        AuthenticationStrategy strategy = strategySelector.getStrategy(authType);
        return strategy.authenticate(request);
    }
}
```

### Decorator Pattern para Logging Automático

```java
@Service
@Primary
public class LoggingAuthenticationDecorator implements AuthenticationStrategy {

    private final AuthenticationStrategy delegate;
    private static final Logger logger = LoggerFactory.getLogger(LoggingAuthenticationDecorator.class);

    @Autowired
    public LoggingAuthenticationDecorator(
            @Qualifier("userAuthenticationService") AuthenticationStrategy delegate) {
        this.delegate = delegate;
    }

    @Override
    public LoginResponse authenticate(LoginRequest loginRequest) {
        logger.info("Iniciando autenticación para: {}", loginRequest.getUsername());
        long startTime = System.currentTimeMillis();

        try {
            LoginResponse response = delegate.authenticate(loginRequest);
            long duration = System.currentTimeMillis() - startTime;
            
            logger.info("Autenticación {} para {} en {}ms",
                response.isSuccess() ? "exitosa" : "fallida",
                loginRequest.getUsername(),
                duration);
            
            return response;
        } catch (Exception e) {
            logger.error("Error en autenticación para {}: ", loginRequest.getUsername(), e);
            throw e;
        }
    }

    @Override
    public boolean isValidSession(String token) {
        return delegate.isValidSession(token);
    }

    @Override
    public String getStrategyType() {
        return delegate.getStrategyType() + "_LOGGED";
    }
}
```

---

## 📚 Mejores Prácticas

1. **Usar AuthService como fachada** en controladores
2. **Inyectar interfaces** (Strategy) en lugar de implementaciones concretas
3. **Usar @Qualifier** cuando haya múltiples implementaciones
4. **Crear tests unitarios** para cada Strategy
5. **Documentar** nuevas implementaciones
6. **Mantener cohesión alta** en cada servicio
7. **Usar logging** para trazabilidad
8. **Validar** antes de delegar

---

## 🔗 Referencias

- [Patrón Strategy](https://refactoring.guru/design-patterns/strategy)
- [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)
- [Spring Dependency Injection](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-dependencies)
