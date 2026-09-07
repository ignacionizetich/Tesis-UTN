package com.EDJ.ArCash.exception;

import com.EDJ.ArCash.exception.personalizated.*;
import com.EDJ.ArCash.exception.response.ApiErrorCode;
import com.EDJ.ArCash.exception.response.ApiFieldError;
import com.EDJ.ArCash.exception.response.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Traduce cualquier excepcion que escape de un controller a un {@link ErrorResponse}.
 *
 * <p>Reglas que sigue todo handler de esta clase:
 * <ul>
 *   <li>El estado HTTP lo determina el {@link ApiErrorCode}, nunca se escribe a mano: eso
 *       hace imposible devolver un 200 con cuerpo de error o un codigo desalineado.</li>
 *   <li>Los 4xx se loguean en {@code WARN} (son errores del cliente y no requieren
 *       intervencion) y los 5xx en {@code ERROR} con el stack trace completo.</li>
 *   <li>Ningun 5xx propaga {@code ex.getMessage()} al cliente: los mensajes de driver JDBC o
 *       de Hibernate revelan nombres de tablas, constraints y consultas. El detalle queda en
 *       el log, correlacionado por {@code traceId}.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String GENERIC_SERVER_ERROR = "Ocurrió un error inesperado. Volvé a intentarlo en unos minutos.";

    // =====================================================================
    // Excepciones de dominio
    // =====================================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return clientError(ApiErrorCode.NOT_FOUND, ex.getMessage(), ex, request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {
        return clientError(ApiErrorCode.BAD_REQUEST, ex.getMessage(), ex, request);
    }

    /**
     * Conflicto con el estado actual del recurso (duplicados de email, DNI, alias).
     *
     * <p>Si el conflicto se puede atribuir a un campo concreto, viaja en {@code fieldErrors}
     * para que el cliente lo resalte igual que un error de validacion.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex, HttpServletRequest request) {
        List<ApiFieldError> fieldErrors = ex.getField() == null
                ? null
                : List.of(new ApiFieldError(ex.getField(), ex.getMessage()));
        return clientError(ApiErrorCode.CONFLICT, ex.getMessage(), ex, request, fieldErrors);
    }

    /**
     * Fallo del servidor con un mensaje que el dominio considera seguro de mostrar.
     * Se loguea como error, igual que cualquier otro 5xx.
     */
    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ErrorResponse> handleInternalServer(
            InternalServerException ex, HttpServletRequest request) {
        return serverError(ApiErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage(), ex, request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        return clientError(ApiErrorCode.UNAUTHORIZED, ex.getMessage(), ex, request);
    }

    /** Cuenta deshabilitada: mismo codigo que emite el filtro JWT para no duplicar contratos. */
    @ExceptionHandler(DisabledAccountException.class)
    public ResponseEntity<ErrorResponse> handleDisabledAccount(
            DisabledAccountException ex, HttpServletRequest request) {
        return clientError(ApiErrorCode.ACCOUNT_DISABLED, ex.getMessage(), ex, request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex, HttpServletRequest request) {
        return clientError(ApiErrorCode.FORBIDDEN, ex.getMessage(), ex, request);
    }

    @ExceptionHandler(PasswordMissmatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMissmatch(
            PasswordMissmatchException ex, HttpServletRequest request) {
        return clientError(ApiErrorCode.CREDENTIALS_MISSMATCH, ex.getMessage(), ex, request);
    }

    /**
     * El proveedor externo de cotizaciones no respondio y tampoco hay valor cacheado. Es un
     * 503 y no un 500: la causa es una dependencia caida y el cliente puede reintentar.
     */
    @ExceptionHandler(ExchangeRateUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleExchangeRateUnavailable(
            ExchangeRateUnavailableException ex, HttpServletRequest request) {
        return clientError(ApiErrorCode.EXCHANGE_RATE_UNAVAILABLE, ex.getMessage(), ex, request);
    }

    // =====================================================================
    // Validacion del request
    // =====================================================================

    /**
     * Falla de Bean Validation sobre un {@code @Valid @RequestBody}.
     *
     * <p>Reune las violaciones de campo y tambien los errores de clase (los que declaran los
     * validadores cruzados, como "la contrasena no puede ser igual al usuario"), que Spring
     * expone como {@link ObjectError} sin campo asociado.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ApiFieldError> fieldErrors = new ArrayList<>();
        for (ObjectError error : ex.getBindingResult().getAllErrors()) {
            String field = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            fieldErrors.add(new ApiFieldError(field, error.getDefaultMessage()));
        }

        return clientError(ApiErrorCode.VALIDATION_ERROR, summarize(fieldErrors), ex, request, fieldErrors);
    }

    /**
     * Falla de Bean Validation sobre parametros sueltos ({@code @RequestParam},
     * {@code @PathVariable}) en una clase anotada con {@code @Validated}.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<ApiFieldError> fieldErrors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            fieldErrors.add(new ApiFieldError(lastNode(violation), violation.getMessage()));
        }

        return clientError(ApiErrorCode.VALIDATION_ERROR, summarize(fieldErrors), ex, request, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "válido";
        String message = String.format("El parámetro '%s' debe ser de tipo %s.", ex.getName(), expected);
        return clientError(ApiErrorCode.TYPE_MISMATCH, message, ex, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        String message = String.format("Falta el parámetro obligatorio '%s'.", ex.getParameterName());
        return clientError(ApiErrorCode.MISSING_PARAMETER, message, ex, request,
                List.of(new ApiFieldError(ex.getParameterName(), "es obligatorio")));
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorResponse> handleMissingCookie(
            MissingRequestCookieException ex, HttpServletRequest request) {

        String message = String.format("Falta la cookie obligatoria '%s'.", ex.getCookieName());
        return clientError(ApiErrorCode.MISSING_PARAMETER, message, ex, request);
    }

    /**
     * JSON sintacticamente invalido o incompatible con el DTO. No se reenvia
     * {@code ex.getMessage()} porque incluye la ruta de clases Java del DTO destino.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return clientError(ApiErrorCode.MALFORMED_JSON,
                "El cuerpo de la petición no es un JSON válido.", ex, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return clientError(ApiErrorCode.ILLEGAL_ARGUMENT, ex.getMessage(), ex, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        return clientError(ApiErrorCode.BAD_REQUEST, ex.getMessage(), ex, request);
    }

    // =====================================================================
    // Autenticacion y autorizacion
    // =====================================================================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        // Mensaje deliberadamente ambiguo: distinguir "usuario inexistente" de "contrasena
        // incorrecta" permitiria enumerar cuentas registradas.
        return clientError(ApiErrorCode.BAD_CREDENTIALS, "Usuario o contraseña incorrectos.", ex, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        return clientError(ApiErrorCode.AUTHENTICATION_ERROR,
                "No pudimos validar tu identidad. Volvé a iniciar sesión.", ex, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return clientError(ApiErrorCode.ACCESS_DENIED,
                "No tenés permisos para acceder a este recurso.", ex, request);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(
            ExpiredJwtException ex, HttpServletRequest request) {
        return clientError(ApiErrorCode.INVALID_TOKEN, "Tu sesión expiró. Volvé a iniciar sesión.", ex, request);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwt(
            JwtException ex, HttpServletRequest request) {
        return clientError(ApiErrorCode.INVALID_TOKEN, "El token es inválido o expiró.", ex, request);
    }

    // =====================================================================
    // Protocolo HTTP y ruteo
    // =====================================================================

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(
            Exception ex, HttpServletRequest request) {
        return clientError(ApiErrorCode.ENDPOINT_NOT_FOUND, "El recurso solicitado no existe.", ex, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        String message = String.format("El método %s no está permitido en este endpoint.", ex.getMethod());
        return clientError(ApiErrorCode.METHOD_NOT_ALLOWED, message, ex, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return clientError(ApiErrorCode.UNSUPPORTED_MEDIA_TYPE,
                "El Content-Type de la petición no está soportado.", ex, request);
    }

    // =====================================================================
    // Persistencia
    // =====================================================================

    /**
     * Violacion de constraint en base. Se responde 409 y se traduce el caso mas frecuente
     * (indice unico) a un mensaje entendible, sin filtrar el nombre del constraint.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String traceId = newTraceId();
        logger.error("[{}] Violación de integridad de datos en {} {}",
                traceId, request.getMethod(), request.getRequestURI(), ex);

        String rootCause = ex.getMostSpecificCause().getMessage();
        boolean duplicated = rootCause != null
                && (rootCause.contains("Duplicate entry") || rootCause.contains("Unique index"));

        String message = duplicated
                ? "Alguno de los datos ingresados ya está registrado."
                : "Los datos enviados no cumplen las restricciones de la base de datos.";

        return build(ApiErrorCode.DATA_INTEGRITY_VIOLATION, message, traceId, request, null);
    }

    /**
     * Dos operaciones concurrentes tocaron la misma fila versionada. Se responde 409 para que
     * el cliente reintente en lugar de un 500 opaco.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(
            OptimisticLockingFailureException ex, HttpServletRequest request) {

        String traceId = newTraceId();
        logger.warn("[{}] Conflicto de concurrencia en {} {}: {}",
                traceId, request.getMethod(), request.getRequestURI(), ex.getMessage());

        return build(ApiErrorCode.CONFLICT,
                "La operación se procesó en paralelo. Volvé a intentarlo.", traceId, request, null);
    }

    // =====================================================================
    // Infraestructura
    // =====================================================================

    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<ErrorResponse> handleMessaging(
            MessagingException ex, HttpServletRequest request) {
        return serverError(ApiErrorCode.EMAIL_ERROR,
                "No pudimos enviar el email. Intentá de nuevo más tarde.", ex, request);
    }

    @ExceptionHandler(UnsupportedEncodingException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedEncoding(
            UnsupportedEncodingException ex, HttpServletRequest request) {
        return serverError(ApiErrorCode.ENCODING_ERROR, GENERIC_SERVER_ERROR, ex, request);
    }

    /**
     * Ultima red de contencion. Cualquier excepcion no prevista se registra completa y el
     * cliente solo recibe un mensaje genérico mas el {@code traceId} para reportarlo.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        return serverError(ApiErrorCode.INTERNAL_SERVER_ERROR, GENERIC_SERVER_ERROR, ex, request);
    }

    // =====================================================================
    // Construccion de la respuesta
    // =====================================================================

    private ResponseEntity<ErrorResponse> clientError(ApiErrorCode code, String message,
                                                      Exception ex, HttpServletRequest request) {
        return clientError(code, message, ex, request, null);
    }

    private ResponseEntity<ErrorResponse> clientError(ApiErrorCode code, String message,
                                                      Exception ex, HttpServletRequest request,
                                                      List<ApiFieldError> fieldErrors) {
        String traceId = newTraceId();
        logger.warn("[{}] {} en {} {}: {}", traceId, code, request.getMethod(),
                request.getRequestURI(), message);
        logger.debug("[{}] Detalle de {}", traceId, code, ex);
        return build(code, message, traceId, request, fieldErrors);
    }

    private ResponseEntity<ErrorResponse> serverError(ApiErrorCode code, String message,
                                                      Exception ex, HttpServletRequest request) {
        String traceId = newTraceId();
        logger.error("[{}] {} en {} {}", traceId, code, request.getMethod(),
                request.getRequestURI(), ex);
        return build(code, message, traceId, request, null);
    }

    private ResponseEntity<ErrorResponse> build(ApiErrorCode code, String message, String traceId,
                                                HttpServletRequest request,
                                                List<ApiFieldError> fieldErrors) {
        String safeMessage = (message == null || message.isBlank())
                ? code.status().getReasonPhrase()
                : message;

        return ResponseEntity
                .status(code.status())
                .body(ErrorResponse.of(code, safeMessage, traceId, request, fieldErrors));
    }

    /**
     * Resume las violaciones en una sola linea para clientes que solo muestran
     * {@code message}, sin obligarlos a recorrer {@code fieldErrors}.
     */
    private String summarize(List<ApiFieldError> fieldErrors) {
        if (fieldErrors.isEmpty()) {
            return "Los datos enviados no son válidos.";
        }
        if (fieldErrors.size() == 1) {
            ApiFieldError only = fieldErrors.get(0);
            return only.field() == null || only.field().isBlank()
                    ? only.message()
                    : only.field() + ": " + only.message();
        }
        return "Hay " + fieldErrors.size() + " campos con datos inválidos.";
    }

    private String lastNode(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }

    private String newTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
