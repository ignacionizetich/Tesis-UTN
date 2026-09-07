package com.EDJ.ArCash.Security;

import com.EDJ.ArCash.Models.User;
import com.EDJ.ArCash.Service.interfaces.SessionService;
import com.EDJ.ArCash.exception.response.ApiErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
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
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final UserDetailsService userDetailsService;
  private final JwtService jwtService;
  private final SessionService sessionService;
  private final ApiErrorWriter apiErrorWriter;

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
                                 SessionService sessionService,
                                 ApiErrorWriter apiErrorWriter) {
    this.userDetailsService = userDetailsService;
    this.jwtService = jwtService;
    this.sessionService = sessionService;
    this.apiErrorWriter = apiErrorWriter;
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
        reject(request, response, ApiErrorCode.INVALID_TOKEN,
          "El token enviado no es un token de acceso.");
        return;
      }

      if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

        User user = ((CustomUserDetails) userDetails).getUser();
        Long userId = user.getId();

        if (!user.isActive()) {
          logger.warn("Cuenta deshabilitada para usuario {}", userId);
          reject(request, response, ApiErrorCode.ACCOUNT_DISABLED,
            "Tu cuenta está deshabilitada. Contactá a soporte técnico.");
          return;
        }

        if (!sessionService.tieneSesionActiva(userId)) {
          logger.warn("Sesion finalizada para usuario {}", userId);
          reject(request, response, ApiErrorCode.SESSION_ENDED,
            "Tu sesión finalizó. Volvé a iniciar sesión.");
          return;
        }

        String role = claims.get("role", String.class);
        List<GrantedAuthority> authorities = buildAuthorities(role);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
          userDetails,
          null,
          authorities
        );

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    } catch (ExpiredJwtException e) {
      logger.warn("Token JWT expirado: {}", e.getMessage());
      reject(request, response, ApiErrorCode.INVALID_TOKEN,
        "Tu sesión expiró. Volvé a iniciar sesión.");
      return;
    } catch (JwtException | IllegalArgumentException e) {
      // IllegalArgumentException la lanza jjwt cuando el token viene vacio o mal formado.
      logger.warn("Token JWT inválido: {}", e.getMessage());
      reject(request, response, ApiErrorCode.INVALID_TOKEN, "El token es inválido o expiró.");
      return;
    }

    filterChain.doFilter(request, response);
  }

  /**
   * Corta la cadena con un {@code ErrorResponse}. Se delega en {@link ApiErrorWriter} para que
   * el cuerpo sea byte a byte el mismo que emite el {@code GlobalExceptionHandler}.
   */
  private void reject(HttpServletRequest request,
                      HttpServletResponse response,
                      ApiErrorCode code,
                      String message) throws IOException {
    SecurityContextHolder.clearContext();
    apiErrorWriter.write(request, response, code, message,
      UUID.randomUUID().toString().substring(0, 8));
  }

  /**
   * ROOT hereda ROLE_ADMIN además de ROLE_ROOT, para no perder acceso a
   * /api/admin/** (mismo criterio que CustomUserDetails.getAuthorities()).
   * Si el día de mañana agregás otro rol con jerarquía, este es el único
   * lugar (junto con CustomUserDetails) que hay que tocar.
   */
  private List<GrantedAuthority> buildAuthorities(String role) {
    if ("ROOT".equals(role)) {
      return List.of(
        new SimpleGrantedAuthority("ROLE_ROOT"),
        new SimpleGrantedAuthority("ROLE_ADMIN")
      );
    }
    return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
  }

}
