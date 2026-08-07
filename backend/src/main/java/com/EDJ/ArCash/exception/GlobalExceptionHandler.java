package com.EDJ.ArCash.exception;

import io.jsonwebtoken.JwtException;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manejador global de excepciones para toda la aplicación
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja excepciones de recursos no encontrados
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        logger.error("Recurso no encontrado: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                false,
                ex.getMessage(),
                "NOT_FOUND",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Maneja excepciones de peticiones incorrectas
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex,
            HttpServletRequest request) {
        
        logger.error("Petición incorrecta: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                false,
                ex.getMessage(),
                "BAD_REQUEST",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja excepciones de conflictos (datos duplicados, etc.)
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(
            ConflictException ex,
            HttpServletRequest request) {
        
        logger.error("Conflicto: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                false,
                ex.getMessage(),
                "CONFLICT",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Maneja excepciones de autorización
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex,
            HttpServletRequest request) {
        
        logger.error("No autorizado: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                false,
                ex.getMessage(),
                "UNAUTHORIZED",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Maneja excepciones de acceso denegado
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(
            ForbiddenException ex,
            HttpServletRequest request) {
        
        logger.error("Acceso denegado: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                false,
                ex.getMessage(),
                "FORBIDDEN",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Maneja excepciones de validación de argumentos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        List<String> errors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.add(fieldName + ": " + errorMessage);
        });
        
        logger.error("Error de validación: {}", errors);
        
        ErrorResponse error = new ErrorResponse(
                false,
                "Error de validación",
                "VALIDATION_ERROR",
                request.getRequestURI(),
                errors
        );
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja excepciones de tipo de argumento incorrecto
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        
        String message = String.format("El parámetro '%s' debe ser de tipo %s",
                ex.getName(), ex.getRequiredType().getSimpleName());
        
        logger.error("Error de tipo de argumento: {}", message);
        
        ErrorResponse error = new ErrorResponse(
                false,
                message,
                "TYPE_MISMATCH",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja excepciones de mensaje HTTP no legible
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        
        logger.error("Mensaje HTTP no legible: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                false,
                "El formato de la petición es inválido",
                "MALFORMED_JSON",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja excepciones de integridad de datos (violación de constraints)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        
        logger.error("Violación de integridad de datos: {}", ex.getMessage());
        
        String message = "Error de integridad de datos";
        if (ex.getMessage() != null && ex.getMessage().contains("Duplicate entry")) {
            message = "El registro ya existe en la base de datos";
        }
        
        ErrorResponse error = new ErrorResponse(
                false,
                message,
                "DATA_INTEGRITY_VIOLATION",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Maneja excepciones de credenciales incorrectas
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {
        
        logger.error("Credenciales incorrectas: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                false,
                "Credenciales incorrectas",
                "BAD_CREDENTIALS",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Maneja excepciones de autenticación
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request) {
        
        logger.error("Error de autenticación: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                false,
                "Error de autenticación",
                "AUTHENTICATION_ERROR",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Maneja excepciones de acceso denegado de Spring Security
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        
        logger.error("Acceso denegado por Spring Security: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                false,
                "No tienes permisos para acceder a este recurso",
                "ACCESS_DENIED",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Maneja excepciones de JWT
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(
            JwtException ex,
            HttpServletRequest request) {
        
        logger.error("Error de JWT: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                false,
                "Token inválido o expirado",
                "INVALID_TOKEN",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Maneja excepciones de handler no encontrado (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex,
            HttpServletRequest request) {
        
        logger.error("Endpoint no encontrado: {}", ex.getRequestURL());
        
        ErrorResponse error = new ErrorResponse(
                false,
                "El endpoint solicitado no existe",
                "ENDPOINT_NOT_FOUND",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Maneja excepciones de mensajes de email
     */
    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<ErrorResponse> handleMessagingException(
            MessagingException ex,
            HttpServletRequest request) {
        
        logger.error("Error al enviar email: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                false,
                "Error al enviar el email",
                "EMAIL_ERROR",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Maneja excepciones de codificación no soportada
     */
    @ExceptionHandler(UnsupportedEncodingException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedEncodingException(
            UnsupportedEncodingException ex,
            HttpServletRequest request) {
        
        logger.error("Codificación no soportada: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                false,
                "Error de codificación",
                "ENCODING_ERROR",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Maneja excepciones de argumentos ilegales
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        
        logger.error("Argumento ilegal: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
                false,
                ex.getMessage(),
                "ILLEGAL_ARGUMENT",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja todas las excepciones genéricas no capturadas
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {
        
        logger.error("Error interno del servidor: ", ex);
        
        ErrorResponse error = new ErrorResponse(
                false,
                "Error interno del servidor",
                "INTERNAL_SERVER_ERROR",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
