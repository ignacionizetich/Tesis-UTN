package com.EDJ.ArCash.Service.support;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class AliasFormatValidator {

    private static final Pattern FORMATO_VALIDO = Pattern.compile(
            "^(?=.*[A-Za-z])(?=^[A-Za-z0-9]+(\\.[A-Za-z0-9]+)+$)(?!.*\\.\\.)[A-Za-z0-9.]{4,25}$");

    public boolean esValido(String alias) {
        return alias != null && FORMATO_VALIDO.matcher(alias).matches();
    }
}
