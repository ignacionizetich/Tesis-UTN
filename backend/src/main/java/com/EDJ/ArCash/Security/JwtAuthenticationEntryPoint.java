package com.EDJ.ArCash.Security;

import com.EDJ.ArCash.exception.response.ApiErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Responde los 401 de peticiones que llegan a un endpoint protegido sin autenticacion valida.
 *
 * <p>Antes usaba {@code response.sendError(401, ...)}, que delega en la pagina de error del
 * contenedor y devuelve HTML: un cliente que hace {@code response.json()} recibia un error de
 * parseo en lugar del motivo del rechazo. Ahora emite el mismo {@code ErrorResponse} que el
 * resto de la API.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    private final ApiErrorWriter apiErrorWriter;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String traceId = UUID.randomUUID().toString().substring(0, 8);
        logger.warn("[{}] Acceso sin autenticación a {} {}: {}",
                traceId, request.getMethod(), request.getRequestURI(), authException.getMessage());

        apiErrorWriter.write(request, response,
                ApiErrorCode.AUTHENTICATION_REQUIRED,
                "Necesitás iniciar sesión para acceder a este recurso.",
                traceId);
    }
}
