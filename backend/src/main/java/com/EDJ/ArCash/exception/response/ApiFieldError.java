package com.EDJ.ArCash.exception.response;

/**
 * Violacion de validacion sobre un campo concreto del request.
 *
 * <p>Deliberadamente no incluye el valor rechazado: los DTO de entrada transportan
 * contrasenas y PINs, y devolverlos en el cuerpo del error los filtraria a los logs
 * del cliente y a las herramientas de red del navegador.
 */
public record ApiFieldError(String field, String message) {
}
