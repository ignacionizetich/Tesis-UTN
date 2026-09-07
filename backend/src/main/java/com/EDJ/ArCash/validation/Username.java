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
 * Formato del nombre de usuario (campo {@code alias} de {@code User}, que se replica como
 * {@code username} en {@code Credentials}).
 *
 * <p>Unifica tres reglas que hasta ahora vivian separadas y en desacuerdo: el formulario de
 * Angular aceptaba {@code [A-Za-z0-9_-]{3,10}}, {@code AliasFormatValidator} exigia formato con
 * puntos y {@code UserServiceImpl.cambiarAliasYUsername} usaba un tercer patron sin puntos. El
 * patron de aca es la union de los tres, de modo que ningun usuario ya registrado queda con un
 * nombre que el propio sistema considere invalido.
 *
 * <p>Reglas: 3 a 25 caracteres, letras, digitos, punto, guion y guion bajo; al menos una letra
 * (para que un nombre de usuario no se confunda con un numero de cuenta) y sin separadores al
 * principio, al final ni repetidos.
 */
@Documented
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@Pattern(
        regexp = "^(?=.*[A-Za-z])[A-Za-z0-9]+([._-][A-Za-z0-9]+)*$",
        message = "El nombre de usuario solo admite letras, números, punto, guion y guion bajo, "
                + "debe incluir al menos una letra y no puede empezar ni terminar con un separador"
)
@jakarta.validation.constraints.Size(
        min = 3, max = 25,
        message = "El nombre de usuario debe tener entre 3 y 25 caracteres"
)
public @interface Username {

    String message() default "El nombre de usuario no tiene un formato válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
