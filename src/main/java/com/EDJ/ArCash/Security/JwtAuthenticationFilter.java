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

    public JwtAuthenticationFilter(@Value("${spring.jwt.secret}") String signedJwt, UserDetailsService userDetailsService) {
        byte[] keyBytes = Base64.getDecoder().decode(signedJwt);
        secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 1. Si no hay header o no empieza con "Bearer ", simplemente continúa.
        // Spring Security se encargará de denegar el acceso si la ruta es protegida.
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

            // 4. Si tenemos username y no hay una autenticación ya establecida en el contexto...
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

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
            logger.error("Error validando el token JWT: {}");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
            response.getWriter().write("{\"error\": \"Token JWT inválido o expirado\"}");
            return; // Detenemos la cadena de filtros
        }

        // 6. Continúa con el siguiente filtro en la cadena
        filterChain.doFilter(request, response);
    }

    private boolean requiresAuthentication(String uri) {
        return !uri.equals("/") &&
                !uri.equals("/home") &&
                !uri.equals("/register") &&
                !uri.equals("/PreLogin") &&
                !uri.startsWith("/css/") &&
                !uri.startsWith("/js/") &&
                !uri.startsWith("/api/auth/") &&
                !uri.startsWith("/api/user/create") &&
                !uri.startsWith("/api/impuestos/") &&
                !uri.equals("/error") &&
                !uri.equals("/validate") &&
                !uri.equals("/forgot") &&
                !uri.equals("/reset-password") &&
                !uri.equals("/api/auth/send-recover-mail") &&
                !uri.equals("/validate-request") &&
                !uri.equals("/swagger-ui.html") &&
                !uri.startsWith("/swagger-ui/") &&
                !(uri.equals("/v3/api-docs") || uri.startsWith("/v3/api-docs/")) &&
                !uri.startsWith("/dashboard") &&
                !uri.startsWith("/adminDashboard");

    }
}