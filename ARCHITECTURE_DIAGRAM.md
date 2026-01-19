# Diagrama de Arquitectura - Patrón Strategy en AuthService

## 🏗️ Arquitectura Antes de la Refactorización

```
┌─────────────────────────────────────────┐
│                                         │
│         AuthService (Monolítico)        │
│                                         │
│  ❌ Múltiples Responsabilidades:        │
│                                         │
│  • Autenticación de usuarios            │
│  • Generación de tokens JWT             │
│  • Validación de tokens                 │
│  • Gestión de refresh tokens            │
│  • Recuperación de contraseñas          │
│  • Envío de emails                      │
│  • Cambio de alias                      │
│                                         │
│  249 líneas de código                   │
│  Baja cohesión, alto acoplamiento       │
│                                         │
└─────────────────────────────────────────┘
```

## 🏗️ Arquitectura Después de la Refactorización

```
┌──────────────────────────────────────────────────────────────────────────┐
│                                                                          │
│                    AuthService (Fachada - 130 líneas)                   │
│                                                                          │
│  ✅ Responsabilidad: Coordinar servicios especializados                 │
│                                                                          │
│  • login(LoginRequest)                                                   │
│  • logout(String)                                                        │
│  • isValidSession(String)                                                │
│  • enviarCorreoRecuperacion(String)                                      │
│  • cambiarAliasYUsername(Long, String)                                   │
│  • tokenValido(String)                                                   │
│  • resendPasswordRecovery(String)                                        │
│                                                                          │
└────────┬──────────────────────┬──────────────────────┬──────────────────┘
         │                      │                      │
         │ Delega               │ Delega               │ Delega
         ▼                      ▼                      ▼
┌─────────────────────┐ ┌─────────────────────┐ ┌─────────────────────┐
│                     │ │                     │ │                     │
│ <<interface>>       │ │ <<interface>>       │ │ <<interface>>       │
│ Authentication      │ │ TokenManagement     │ │ PasswordRecovery    │
│ Strategy            │ │ Strategy            │ │ Strategy            │
│                     │ │                     │ │                     │
└──────────┬──────────┘ └──────────┬──────────┘ └──────────┬──────────┘
           │                       │                       │
           │ Implementa            │ Implementa            │ Implementa
           ▼                       ▼                       ▼
┌─────────────────────┐ ┌─────────────────────┐ ┌─────────────────────┐
│                     │ │                     │ │                     │
│ User                │ │ Jwt                 │ │ EmailPassword       │
│ Authentication      │ │ TokenManagement     │ │ Recovery            │
│ Service             │ │ Service             │ │ Service             │
│                     │ │                     │ │                     │
│ Responsabilidad:    │ │ Responsabilidad:    │ │ Responsabilidad:    │
│ • Validar           │ │ • Generar tokens    │ │ • Enviar emails     │
│   credenciales      │ │ • Guardar tokens    │ │ • Validar tokens    │
│ • Verificar usuario │ │ • Revocar tokens    │ │ • Reenviar links    │
│ • Validar sesión    │ │ • Extraer userId    │ │                     │
│                     │ │                     │ │                     │
│ 120 líneas          │ │ 130 líneas          │ │ 100 líneas          │
│                     │ │                     │ │                     │
└─────────┬───────────┘ └─────────┬───────────┘ └─────────┬───────────┘
          │                       │                       │
          │ Usa                   │ Usa                   │ Usa
          ▼                       ▼                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│                    Repositorios y Utilidades                    │
│                                                                 │
│  • CredentialRepository    • JwtUtils                          │
│  • UserRepository          • EventPublisher                     │
│  • AccountRepository       • RecoveryTokenService               │
│  • RefreshTokenRepository  • PasswordEncoder                    │
│  • RecoveryTokenRepository                                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 🔄 Flujo de Login (Ejemplo)

```
Cliente
  │
  │ 1. POST /api/auth/login
  ▼
┌─────────────────┐
│  AuthController │
└────────┬────────┘
         │
         │ 2. authService.login(request)
         ▼
┌─────────────────┐
│  AuthService    │ (Fachada)
└────────┬────────┘
         │
         │ 3. authenticationStrategy.authenticate(request)
         ▼
┌──────────────────────────┐
│ UserAuthenticationService│
└────────┬─────────────────┘
         │
         │ 4. Validar credenciales
         │ 5. Verificar usuario activo
         ▼
┌──────────────────────────┐
│ JwtTokenManagementService│
└────────┬─────────────────┘
         │
         │ 6. Generar/recuperar tokens
         │ 7. Guardar refresh token
         │
         ▼
LoginResponse ──► AuthController ──► Cliente
```

## 📊 Comparación de Responsabilidades

### Antes
```
AuthService
├── Autenticación        (40%)
├── Tokens               (30%)
├── Recuperación         (20%)
└── Cambio de alias      (10%)

Total: 100% en una clase
Cohesión: ⭐⭐☆☆☆
```

### Después
```
UserAuthenticationService
└── Autenticación        (100%)
    Cohesión: ⭐⭐⭐⭐⭐

JwtTokenManagementService
└── Tokens               (100%)
    Cohesión: ⭐⭐⭐⭐⭐

EmailPasswordRecoveryService
└── Recuperación         (100%)
    Cohesión: ⭐⭐⭐⭐⭐

AuthService (Fachada)
└── Coordinación         (100%)
    Cohesión: ⭐⭐⭐⭐⭐
```

## 🚀 Extensibilidad

### Agregar OAuth (Ejemplo)

```
┌──────────────────────┐
│ <<interface>>        │
│ Authentication       │
│ Strategy             │
└──────────┬───────────┘
           │
           ├─ UserAuthenticationService
           │
           └─ OAuthAuthenticationService  ← NUEVA
                  │
                  ├─ GoogleOAuthProvider
                  ├─ FacebookOAuthProvider
                  └─ GitHubOAuthProvider
```

### Agregar 2FA (Ejemplo)

```
┌──────────────────────┐
│ <<interface>>        │
│ Authentication       │
│ Strategy             │
└──────────┬───────────┘
           │
           ├─ UserAuthenticationService
           │
           └─ TwoFactorAuthenticationService  ← NUEVA
                  │
                  ├─ EmailTwoFactorProvider
                  ├─ SmsTwoFactorProvider
                  └─ AppTwoFactorProvider
```

## 💡 Beneficios Clave

### 1. Alta Cohesión
```
Antes: Una clase hace todo
Después: Cada clase hace UNA cosa bien
```

### 2. Bajo Acoplamiento
```
Antes: Cambios en tokens afectan autenticación
Después: Cambios aislados en cada servicio
```

### 3. Testabilidad
```
Antes: Mock de 10+ dependencias
Después: Mock solo de lo necesario por servicio
```

### 4. Mantenibilidad
```
Antes: 249 líneas para entender todo
Después: ~120 líneas por servicio especializado
```

### 5. Extensibilidad
```
Antes: Modificar código existente
Después: Agregar nueva implementación de interfaz
```

## 🎯 Principios SOLID Aplicados

✅ **S**ingle Responsibility Principle
- Cada servicio tiene UNA responsabilidad

✅ **O**pen/Closed Principle
- Abierto a extensión (nuevas strategies)
- Cerrado a modificación

✅ **L**iskov Substitution Principle
- Cualquier implementación de Strategy es intercambiable

✅ **I**nterface Segregation Principle
- Interfaces específicas y enfocadas

✅ **D**ependency Inversion Principle
- AuthService depende de abstracciones (interfaces)
