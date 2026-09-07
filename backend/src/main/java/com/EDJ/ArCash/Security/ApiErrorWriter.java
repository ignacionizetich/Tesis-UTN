package com.EDJ.ArCash.Security;

import com.EDJ.ArCash.exception.response.ApiErrorCode;
import com.EDJ.ArCash.exception.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Escribe un {@link ErrorResponse} directamente en la respuesta HTTP.
 *
 * <p>La cadena de filtros de Spring Security corre antes del {@code DispatcherServlet}, asi que
 * los rechazos de autenticacion nunca llegan al {@code GlobalExceptionHandler}. Sin este
 * componente cada punto de fallo arma su propio JSON a mano y el cliente termina recibiendo
 * formatos distintos segun por donde falle: texto plano de {@code sendError}, un
 * {@code {"error": "..."}} improvisado o el {@code ErrorResponse} del advice.
 *
 * <p>Reutiliza el {@link ObjectMapper} de Spring Boot para que la serializacion de fechas sea
 * identica a la de los controllers.
 */
@Component
@RequiredArgsConstructor
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletRequest request,
                      HttpServletResponse response,
                      ApiErrorCode code,
                      String message,
                      String traceId) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse body = ErrorResponse.of(code, message, traceId, request);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}
