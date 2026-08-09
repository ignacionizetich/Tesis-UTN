package com.EDJ.ArCash.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.security.Key;
import java.util.Base64;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final Key secretKey;
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    public JwtAuthenticationFilter(@Value("${spring.jwt.secret}") String signedJwt,
                                   UserDetailsService userDetailsService,
                                   JwtUtils jwtUtils) {
        byte[] keyBytes = Base64.getDecoder().decode(signedJwt);
        secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

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
            // 3. Validar el token y extraer el username (subject)
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();

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