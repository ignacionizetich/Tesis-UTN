package com.EDJ.ArCash.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Impide que la contrasena reproduzca datos de identidad del propio usuario (nombre de usuario,
 * email, DNI, nombre y apellido).
 *
 * <p>Es una restriccion a nivel de clase porque necesita comparar varios campos entre si, algo
 * que un validador de campo no puede hacer: cuando Bean Validation visita {@code password} no
 * tiene forma de ver el {@code alias} del mismo DTO.
 *
 * <p>Los campos se indican por nombre de propiedad, de modo que el mismo validador sirve para
 * el registro publico y para el alta de administradores sin que los DTO tengan que implementar
 * ninguna interfaz comun.
 *
 * @see PasswordNotSimilarToIdentityValidator
 */
@Documented
@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = PasswordNotSimilarToIdentityValidator.class)
public @interface PasswordNotSimilarToIdentity {

    /** Propiedad que contiene la contrasena en claro. */
    String passwordField() default "password";

    /** Propiedades cuyo valor la contrasena no puede reproducir. */
    String[] identityFields();

    /**
     * Longitud minima que debe tener un dato de identidad para compararlo por inclusion.
     * Terminos muy cortos generarian falsos positivos: un apellido de dos letras aparece por
     * casualidad en casi cualquier contrasena.
     */
    int minTermLength() default 4;

    String message() default "La contraseña no puede coincidir con tus datos personales";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
