package com.EDJ.ArCash.exception.personalizated;

/**
 * La cuenta existe pero esta deshabilitada, asi que no puede operar.
 *
 * <p>Se separa de {@link UnauthorizedException} para que la respuesta lleve el codigo
 * {@code ACCOUNT_DISABLED}: es el mismo que emite el filtro JWT ante un usuario inactivo,
 * y el frontend lo necesita para cerrar la sesion en lugar de reintentar el login.
 */
public class DisabledAccountException extends RuntimeException {

    public DisabledAccountException(String message) {
        super(message);
    }
}
