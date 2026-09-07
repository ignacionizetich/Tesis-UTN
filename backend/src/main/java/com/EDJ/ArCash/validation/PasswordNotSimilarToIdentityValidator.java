package com.EDJ.ArCash.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.NotReadablePropertyException;

/**
 * Compara la contrasena contra los datos de identidad del DTO.
 *
 * <p>Rechaza tres situaciones distintas:
 * <ol>
 *   <li>La contrasena es igual a un dato de identidad ({@code juan.perez} / {@code juan.perez}).</li>
 *   <li>Es igual a la parte local del email ({@code juan.perez@mail.com} → {@code juan.perez}).</li>
 *   <li>Contiene un dato de identidad suficientemente largo ({@code juan.perez2024!}), que es el
 *       patron real que usa la gente para esquivar una comprobacion de igualdad exacta.</li>
 * </ol>
 *
 * <p>Todas las comparaciones son sin distinguir mayusculas: {@code JUAN.PEREZ} no aporta
 * ninguna seguridad frente a {@code juan.perez}.
 */
public class PasswordNotSimilarToIdentityValidator
        implements ConstraintValidator<PasswordNotSimilarToIdentity, Object> {

    private String passwordField;
    private String[] identityFields;
    private int minTermLength;
    private String message;

    @Override
    public void initialize(PasswordNotSimilarToIdentity constraint) {
        this.passwordField = constraint.passwordField();
        this.identityFields = constraint.identityFields();
        this.minTermLength = constraint.minTermLength();
        this.message = constraint.message();
    }

    @Override
    public boolean isValid(Object dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        BeanWrapperImpl wrapper = new BeanWrapperImpl(dto);
        String password = readProperty(wrapper, passwordField);
        if (password == null || password.isBlank()) {
            return true;
        }

        String normalizedPassword = password.toLowerCase();

        for (String field : identityFields) {
            String rawValue = readProperty(wrapper, field);
            if (rawValue == null || rawValue.isBlank()) {
                continue;
            }

            String value = rawValue.trim().toLowerCase();
            if (matches(normalizedPassword, value)) {
                return reject(context, field);
            }

            // El email se compara ademas por su parte local: la contrasena "juan.perez" es
            // igual de adivinable para la cuenta "juan.perez@gmail.com".
            int at = value.indexOf('@');
            if (at > 0 && matches(normalizedPassword, value.substring(0, at))) {
                return reject(context, field);
            }
        }

        return true;
    }

    private boolean matches(String password, String term) {
        if (term.isBlank()) {
            return false;
        }
        if (password.equals(term)) {
            return true;
        }
        return term.length() >= minTermLength && password.contains(term);
    }

    /**
     * Asocia la violacion al campo de la contrasena y no a la clase, para que el frontend pueda
     * pintar el error debajo del input correcto en lugar de mostrarlo como error de formulario.
     */
    private boolean reject(ConstraintValidatorContext context, String identityField) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message + " (" + identityField + ")")
                .addPropertyNode(passwordField)
                .addConstraintViolation();
        return false;
    }

    private String readProperty(BeanWrapperImpl wrapper, String property) {
        try {
            Object value = wrapper.getPropertyValue(property);
            return value != null ? value.toString() : null;
        } catch (NotReadablePropertyException e) {
            // Un nombre de propiedad mal escrito en la anotacion no debe hacer fallar la
            // peticion: se ignora ese termino y el resto de la validacion sigue aplicando.
            return null;
        }
    }
}
