package com.EDJ.ArCash.factory;

/**
 * Interfaz genérica para la creación de Response DTOs
 * Utilizada para respuestas simples que siguen el patrón success + message
 * @param <T> Tipo del Response DTO
 */
public interface ResponseFactory<T> {

    /**
     * Crea una respuesta exitosa
     * @param message Mensaje descriptivo de éxito
     * @return Response exitoso
     */
    T createSuccessResponse(String message);

    /**
     * Crea una respuesta de error
     * @param message Mensaje descriptivo del error
     * @return Response con error
     */
    T createErrorResponse(String message);
}
