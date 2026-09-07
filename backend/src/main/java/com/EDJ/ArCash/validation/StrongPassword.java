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
 * Exige que la contrasena cumpla la politica de fortaleza de la aplicacion.
 *
 * <p>El formulario de Angular ya valida estas reglas, pero esa validacion es solo una ayuda de
 * UX: cualquiera puede saltearla con curl o Postman y llegar directo al endpoint. Repetirlas en
 * el servidor es lo que las convierte en una garantia real.
 *
 * @see StrongPasswordValidator
 */
@Documented
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {

    String message() default "La contraseña no cumple los requisitos de seguridad";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
