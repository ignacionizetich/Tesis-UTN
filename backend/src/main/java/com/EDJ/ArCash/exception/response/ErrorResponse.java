package com.EDJ.ArCash.exception.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuerpo unico de todas las respuestas de error de la API.
 *
 * <p>Es un record inmutable: una vez que el {@code @ExceptionHandler} lo construye, ninguna
 * capa posterior puede mutarlo. Los campos nulos no se serializan, asi que un error simple
 * viaja sin la clave {@code fieldErrors}.
 *
 * <p>Contrato:
 * <ul>
 *   <li>{@code code} es estable y legible por maquina ({@link ApiErrorCode}).</li>
 *   <li>{@code message} esta pensado para mostrarse al usuario final.</li>
 *   <li>{@code traceId} es el correlativo que tambien queda en el log del servidor, para
 *       poder rastrear un error reportado por un usuario sin exponerle el stack trace.</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(

        boolean success,

        int status,

        String code,

        String message,

        String path,

        String method,

        String traceId,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,

        List<ApiFieldError> fieldErrors
) {

    public static ErrorResponse of(ApiErrorCode code,
                                   String message,
                                   String traceId,
                                   HttpServletRequest request) {
        return build(code, message, traceId, request, null);
    }

    public static ErrorResponse of(ApiErrorCode code,
                                   String message,
                                   String traceId,
                                   HttpServletRequest request,
                                   List<ApiFieldError> fieldErrors) {
        return build(code, message, traceId, request,
                fieldErrors == null || fieldErrors.isEmpty() ? null : List.copyOf(fieldErrors));
    }

    private static ErrorResponse build(ApiErrorCode code,
                                       String message,
                                       String traceId,
                                       HttpServletRequest request,
                                       List<ApiFieldError> fieldErrors) {
        return new ErrorResponse(
                false,
                code.status().value(),
                code.name(),
                message,
                request != null ? request.getRequestURI() : null,
                request != null ? request.getMethod() : null,
                traceId,
                LocalDateTime.now(),
                fieldErrors
        );
    }
}
