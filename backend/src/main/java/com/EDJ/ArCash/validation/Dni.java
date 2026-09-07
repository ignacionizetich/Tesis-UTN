package com.EDJ.ArCash.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Formato de DNI argentino: 7 u 8 digitos, sin puntos ni separadores.
 *
 * <p>Se admiten 7 digitos porque los documentos emitidos antes de 1970 los tienen, aunque el
 * formulario actual del frontend exija 8. Hasta ahora el backend solo comprobaba unicidad, asi
 * que un DNI como {@code "abc"} o {@code "1"} se persistia sin objeciones.
 */
@Documented
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@Pattern(regexp = "^\\d{7,8}$", message = "El DNI debe tener 7 u 8 dígitos, sin puntos")
public @interface Dni {

    String message() default "El DNI no tiene un formato válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
