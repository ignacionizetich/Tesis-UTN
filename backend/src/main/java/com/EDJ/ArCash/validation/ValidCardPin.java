package com.EDJ.ArCash.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * PIN de tarjeta: exactamente 6 digitos y no trivialmente adivinable.
 *
 * @see ValidCardPinValidator
 */
@Documented
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidCardPinValidator.class)
public @interface ValidCardPin {

    /** Cantidad exacta de digitos que debe tener el PIN. */
    int length() default 6;

    String message() default "El PIN no es válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
