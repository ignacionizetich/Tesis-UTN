package com.EDJ.ArCash.Service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Valida el formato de un alias elegido por el usuario: entre 4 y 25 caracteres,
 * solo letras, numeros y puntos, al menos un punto en el medio, con alguna letra
 * y sin puntos consecutivos.
 */
@Component
public class AliasFormatValidator {

    private static final Pattern FORMATO_VALIDO = Pattern.compile(
            "^(?=.*[A-Za-z])(?=^[A-Za-z0-9]+(\\.[A-Za-z0-9]+)+$)(?!.*\\.\\.)[A-Za-z0-9.]{4,25}$");

    public boolean esValido(String alias) {
        return alias != null && FORMATO_VALIDO.matcher(alias).matches();
    }
}
