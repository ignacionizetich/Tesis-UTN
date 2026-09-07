package com.EDJ.ArCash.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Nombre o apellido de una persona: 2 a 50 caracteres, solo letras (con acentos y enie),
 * espacios, apostrofos y guiones.
 *
 * <p>Acepta acentos deliberadamente: un patron {@code [A-Za-z]+} rechazaria apellidos
 * perfectamente validos como {@code Muñoz} o {@code Iñíguez}.
 */
@Documented
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@Pattern(
        regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+([ '\\-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$",
        message = "Solo admite letras, espacios, apóstrofos y guiones"
)
@Size(min = 2, max = 50, message = "Debe tener entre 2 y 50 caracteres")
public @interface PersonName {

    String message() default "El nombre no tiene un formato válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
