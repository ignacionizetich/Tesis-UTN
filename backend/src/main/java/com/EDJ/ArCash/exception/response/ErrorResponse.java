package com.EDJ.ArCash.exception.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para respuestas de error estandarizadas
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    private boolean success;
    private String message;
    private String error;
    private String path;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private List<String> details;

    public ErrorResponse(boolean success, String message, String error, String path) {
        this.success = success;
        this.message = message;
        this.error = error;
        this.path = path;
        this.timestamp = LocalDateTime.now();
        this.details = null;
    }

    public ErrorResponse(boolean success, String message, String error, String path, List<String> details) {
        this.success = success;
        this.message = message;
        this.error = error;
        this.path = path;
        this.timestamp = LocalDateTime.now();
        this.details = details;
    }
}
