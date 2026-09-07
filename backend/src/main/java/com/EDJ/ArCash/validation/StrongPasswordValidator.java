package com.EDJ.ArCash.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Verifica la politica de contrasenas y explica exactamente que falta.
 *
 * <p>Un mensaje generico del tipo "contrasena insegura" obliga al usuario a adivinar; el
 * validador acumula todos los incumplimientos y arma un unico mensaje con la lista.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    /** Minimo alineado con el validador del formulario de registro del frontend. */
    public static final int MIN_LENGTH = 8;

    /**
     * BCrypt solo considera los primeros 72 bytes de la entrada: todo lo que exceda ese limite
     * se descarta en silencio, de modo que dos contrasenas larguisimas con el mismo prefijo
     * resultarian equivalentes al validar el login. Se rechaza explicitamente en lugar de
     * dejar que el encoder trunque.
     */
    public static final int MAX_LENGTH = 72;

    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s");

    /**
     * Contrasenas triviales que, pese a todo, aprueban los requisitos de composicion. Sin esta
     * lista "Password1!" seria aceptada como contrasena fuerte.
     */
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password1!", "password123!", "passw0rd!", "qwerty123!", "qwerty1234!",
            "abcd1234!", "admin123!", "arcash123!", "argentina1!", "contrasena1!",
            "contraseña1!", "1qaz2wsx!", "iloveyou1!", "welcome1!", "aa123456!"
    );

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Un valor ausente es responsabilidad de @NotBlank: si tambien lo reportara este
        // validador, el usuario recibiria dos errores para el mismo campo vacio.
        if (password == null || password.isEmpty()) {
            return true;
        }

        List<String> problems = new ArrayList<>();

        if (password.length() < MIN_LENGTH) {
            problems.add("debe tener al menos " + MIN_LENGTH + " caracteres");
        }
        if (password.length() > MAX_LENGTH) {
            problems.add("no puede superar los " + MAX_LENGTH + " caracteres");
        }
        if (!LOWERCASE.matcher(password).find()) {
            problems.add("debe incluir una minúscula");
        }
        if (!UPPERCASE.matcher(password).find()) {
            problems.add("debe incluir una mayúscula");
        }
        if (!DIGIT.matcher(password).find()) {
            problems.add("debe incluir un número");
        }
        if (!SPECIAL.matcher(password).find()) {
            problems.add("debe incluir un carácter especial");
        }
        if (WHITESPACE.matcher(password).find()) {
            problems.add("no puede contener espacios");
        }
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            problems.add("es una contraseña demasiado común");
        }

        if (problems.isEmpty()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("La contraseña " + String.join(", ", problems))
                .addConstraintViolation();
        return false;
    }
}
