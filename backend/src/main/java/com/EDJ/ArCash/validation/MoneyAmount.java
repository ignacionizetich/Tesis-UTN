package com.EDJ.ArCash.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Importe monetario valido para una operacion: estrictamente positivo, con dos decimales como
 * maximo y por debajo de un techo razonable.
 *
 * <p>Las tres reglas cubren casos borde distintos que antes pasaban sin control:
 * <ul>
 *   <li><b>Positivo</b>: un importe negativo invierte el sentido del movimiento. En una compra
 *       de dolares, {@code -10000} acreditaba pesos en lugar de debitarlos.</li>
 *   <li><b>Dos decimales</b>: {@code 0.001} se acumula como saldo imposible de representar en
 *       pesos y arrastra errores de redondeo en cada operacion posterior.</li>
 *   <li><b>Techo</b>: evita que un importe absurdo desborde la aritmetica en coma flotante y
 *       llegue a {@code Infinity}, que persistido deja la cuenta en un estado irrecuperable.</li>
 * </ul>
 */
@Documented
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
@DecimalMax(value = "999999999999.99", message = "El monto excede el máximo permitido")
@Digits(integer = 12, fraction = 2, message = "El monto admite como máximo 2 decimales")
public @interface MoneyAmount {

    String message() default "El monto no es válido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
