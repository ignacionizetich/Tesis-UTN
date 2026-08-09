package com.EDJ.ArCash.exception;

/**
 * Excepción lanzada cuando no se puede obtener la cotización del dólar
 * desde el proveedor externo y tampoco hay un valor cacheado disponible
 */
public class ExchangeRateUnavailableException extends RuntimeException {

    public ExchangeRateUnavailableException(String message) {
        super(message);
    }
}
