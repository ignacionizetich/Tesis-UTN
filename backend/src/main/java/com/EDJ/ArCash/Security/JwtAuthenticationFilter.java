package com.EDJ.ArCash.Security;

import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Service.SessionService;
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
    private final JwtService jwtService;
    private final SessionService sessionService;

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

    public JwtAuthenticationFilter(UserDetailsService userDetailsService,
                                   JwtService jwtService,
                                   SessionService sessionService) {
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
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

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            Claims claims = jwtService.getClaimJWT(jwt);

            username = claims.getSubject();

            String tokenType = claims.get(JwtService.CLAIM_TYPE, String.class);
            if (!JwtService.TYPE_ACCESS.equals(tokenType)) {
                logger.warn("Token rechazado: type esperado '{}', recibido '{}'", JwtService.TYPE_ACCESS, tokenType);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Token de acceso inválido\"}");
                return;
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                User user = ((CustomUserDetails) userDetails).getUser();
                Long userId = user.getId();

                if (!user.isActive()) {
                    logger.warn("Cuenta deshabilitada para usuario {}", userId);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Cuenta deshabilitada\"}");
                    return;
                }

                if (!sessionService.tieneSesionActiva(userId)) {
                    logger.warn("Sesion finalizada para usuario {}", userId);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Sesión finalizada\"}");
                    return;
                }

                String role = claims.get("role", String.class);
                String springRole = "ROLE_" + role;

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(springRole))
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (JwtException e) {
            logger.error("Error validando el token JWT: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token JWT inválido o expirado\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

}
