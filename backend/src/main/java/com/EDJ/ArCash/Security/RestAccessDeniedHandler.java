package com.EDJ.ArCash.Security;

import com.EDJ.ArCash.exception.response.ApiErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Responde los 403 que resuelve la cadena de filtros (por ejemplo, un usuario con
 * {@code ROLE_USER} pidiendo {@code /api/admin/**}).
 *
 * <p>Sin este handler, Spring Security cae en su implementacion por defecto y devuelve una
 * pagina de error del contenedor, distinta del 403 que produce el
 * {@code GlobalExceptionHandler} cuando el rechazo lo lanza un {@code @PreAuthorize}.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    private final ApiErrorWriter apiErrorWriter;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        String traceId = UUID.randomUUID().toString().substring(0, 8);
        logger.warn("[{}] Permisos insuficientes en {} {}: {}",
                traceId, request.getMethod(), request.getRequestURI(),
                accessDeniedException.getMessage());

        apiErrorWriter.write(request, response,
                ApiErrorCode.ACCESS_DENIED,
                "No tenés permisos para acceder a este recurso.",
                traceId);
    }
}
