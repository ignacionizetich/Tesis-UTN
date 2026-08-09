package com.EDJ.ArCash.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;

    /**
     * Debe coincidir con el permitAll de SecurityConfig. En estas rutas el
     * Authorization header se ignora: un access token vencido (como el que
     * manda el interceptor de Angular a /api/auth/refresh) no puede bloquear
     * un endpoint publico.
     */
    private final RequestMatcher rutasPublicas = new OrRequestMatcher(List.of(
            new AntPathRequestMatcher("/**", HttpMethod.OPTIONS.name()),
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/swagger-ui.html"),
            new AntPathRequestMatcher("/v3/api-docs/**"),
            new AntPathRequestMatcher("/swagger-resources/**"),
            new AntPathRequestMatcher("/webjars/**"),
            new AntPathRequestMatcher("/api/auth/login"),
            new AntPathRequestMatcher("/api/auth/refresh"),
            new AntPathRequestMatcher("/api/user/create"),
            new AntPathRequestMatcher("/api/auth/validate"),
            new AntPathRequestMatcher("/api/auth/send-recover-mail"),
            new AntPathRequestMatcher("/api/auth/validate-recovery-token"),
            new AntPathRequestMatcher("/api/auth/reset-password"),
            new AntPathRequestMatcher("/api/resend/**")
    ));

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    public JwtAuthenticationFilter(UserDetailsService userDetailsService, JwtUtils jwtUtils) {
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (rutasPublicas.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 1. Si no hay header o no empieza con "Bearer ", simplemente continúa.
        // Spring Security (SecurityConfig.java) se encargará de denegar el acceso
        // si la ruta no está en la lista de .permitAll()
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extraer el token
        jwt = authHeader.substring(7);

        try {
            // 3. Validar firma/expiracion con la misma clave que firma JwtUtils
            Claims claims = jwtUtils.getClaimJWT(jwt);

            username = claims.getSubject();

            // Solo los access tokens pueden autenticar rutas protegidas.
            // Sin claim type (tokens viejos) o con type=refresh se rechazan.
            String tokenType = claims.get(JwtUtils.CLAIM_TYPE, String.class);
            if (!JwtUtils.TYPE_ACCESS.equals(tokenType)) {
                logger.warn("Token rechazado: type esperado '{}', recibido '{}'", JwtUtils.TYPE_ACCESS, tokenType);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Token de acceso inválido\"}");
                return;
            }

            // 4. Si tenemos username y no hay una autenticación ya establecida en el contexto...
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // Firma y expiracion ya pasaron; falta comprobar que la sesion
                // no haya sido cerrada (logout / revocacion del refresh token).
                Long userId = ((CustomUserDetails) userDetails).getUser().getId();
                if (!jwtUtils.tieneSesionActiva(userId)) {
                    logger.warn("Sesion finalizada para usuario {}", userId);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Sesión finalizada\"}");
                    return;
                }

                // Reconstruimos la autenticación con los roles/authorities
                String role = claims.get("role", String.class);
                String springRole = "ROLE_" + role;

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(springRole))
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Establecemos la autenticación en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (JwtException e) {
            // 5. Si el token es inválido (expirado, malformado, etc.), responde con un error 401
            logger.error("Error validando el token JWT: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token JWT inválido o expirado\"}");
            return;
        }

        // 6. Continúa con el siguiente filtro en la cadena
        filterChain.doFilter(request, response);
    }

}
