package com.EDJ.ArCash.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Rechaza los PIN que cumplen el formato pero no aportan seguridad real.
 *
 * <p>Un PIN de 6 digitos tiene un millon de combinaciones, pero en la practica una fraccion
 * enorme de los usuarios elige uno de un punado de patrones. Se descartan tres familias:
 * <ul>
 *   <li>Todos los digitos iguales ({@code 000000}, {@code 111111}).</li>
 *   <li>Secuencias consecutivas, ascendentes o descendentes ({@code 123456}, {@code 654321}).</li>
 *   <li>PIN de uso masivo documentado ({@code 123123}, {@code 112233}).</li>
 * </ul>
 * Esto importa mas que en otros campos porque el PIN es lo unico que protege el endpoint que
 * revela el numero completo de la tarjeta y su CVC.
 */
public class ValidCardPinValidator implements ConstraintValidator<ValidCardPin, String> {

    private static final java.util.Set<String> BLACKLIST = java.util.Set.of(
            "123123", "121212", "112233", "123321", "696969",
            "159753", "147258", "102030", "123654", "789456"
    );

    private int length;

    @Override
    public void initialize(ValidCardPin constraint) {
        this.length = constraint.length();
    }

    @Override
    public boolean isValid(String pin, ConstraintValidatorContext context) {
        if (pin == null || pin.isEmpty()) {
            return true;
        }

        if (pin.length() != length || !pin.chars().allMatch(Character::isDigit)) {
            return reject(context, "El PIN debe tener exactamente " + length + " dígitos");
        }
        if (allSameDigit(pin)) {
            return reject(context, "El PIN no puede tener todos los dígitos iguales");
        }
        if (isSequential(pin)) {
            return reject(context, "El PIN no puede ser una secuencia de dígitos consecutivos");
        }
        if (BLACKLIST.contains(pin)) {
            return reject(context, "El PIN elegido es demasiado común, probá con otro");
        }
        return true;
    }

    private boolean allSameDigit(String pin) {
        return pin.chars().distinct().count() == 1;
    }

    private boolean isSequential(String pin) {
        boolean ascending = true;
        boolean descending = true;
        for (int i = 1; i < pin.length(); i++) {
            int delta = pin.charAt(i) - pin.charAt(i - 1);
            if (delta != 1) {
                ascending = false;
            }
            if (delta != -1) {
                descending = false;
            }
        }
        return ascending || descending;
    }

    private boolean reject(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
