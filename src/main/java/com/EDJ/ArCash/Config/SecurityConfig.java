package com.EDJ.ArCash.Config;

import com.EDJ.ArCash.Security.JwtAuthenticationEntryPoint;
import com.EDJ.ArCash.Security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Orígenes permitidos (tu Angular y Swagger si lo usas)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",
                "http://localhost:8080" // O la URL de tu Swagger UI
        ));

        // Métodos permitidos (incluye OPTIONS)
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Headers que tu frontend PUEDE ENVIAR
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",        // Para el token JWT
                "Content-Type",       // Para enviar JSON (POST/PUT)
                "Accept",             // Estándar
                "Origin",             // Estándar
                "X-Requested-With",   // Común
                // Headers de caché (por si los usas o alguna librería los añade)
                "Cache-Control",
                "Pragma",
                "Expires",
                // Headers necesarios para CORS preflight
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Headers que tu frontend PUEDE LEER de la respuesta del backend
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", // Si alguna vez necesitas leer un token renovado de la respuesta
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
                // Puedes agregar otros si tu backend los envía y el frontend los necesita
        ));

        // Permitir credenciales (importante para tokens/cookies)
        configuration.setAllowCredentials(true);

        // Tiempo máximo que el navegador cachea la respuesta preflight (OPTIONS)
        configuration.setMaxAge(3600L); // 1 hora

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica esta configuración a TODAS las rutas de tu API
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)

                // Configura el manejo de excepciones de autenticación
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/login", "/api/user/create",
                                "/api/auth/send-recover-mail",
                                "/api/resend/validation", "/api/resend/password-recovery",
                                "/reset-password", "/validate", "/forgot",
                                "/validate-request", "/validate-recovery-token"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}