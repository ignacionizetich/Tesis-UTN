package com.EDJ.ArCash.validation;

import com.EDJ.ArCash.DTO.NonAuthDTO.RegistrerRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fija las reglas de validación del registro.
 *
 * <p>El formulario de Angular replica estas mismas reglas en
 * {@code shared/validators/auth.validators.ts} para no dejar que el usuario complete todo el
 * registro y recién entonces recibir un rechazo. Los casos de acá son los mismos que cubre el
 * spec de ese archivo: si una regla cambia de un lado, este test o el otro se caen y obligan a
 * sincronizar los dos.
 */
class RegistrationValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("Un registro con todos los datos válidos no genera violaciones")
    void registroValido() {
        assertTrue(validate(valido().build()).isEmpty());
    }

    // -----------------------------------------------------------------
    // Política de contraseña
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Contraseña sin mayúscula, número o símbolo: rechazada")
    void contrasenaSinComposicionMinima() {
        assertHasViolation("password", valido().password("solominusculas").build());
    }

    @Test
    @DisplayName("Contraseña con espacios: rechazada")
    void contrasenaConEspacios() {
        assertHasViolation("password", valido().password("Arcash 2026$ok").build());
    }

    @Test
    @DisplayName("Contraseña demasiado común: rechazada aunque cumpla la composición")
    void contrasenaComun() {
        // Tiene mayúscula, minúscula, número y símbolo, y es de las primeras de un diccionario.
        assertHasViolation("password", valido().password("Password1!").build());
    }

    @Test
    @DisplayName("Contraseña más larga de lo que BCrypt considera: rechazada, no truncada")
    void contrasenaDemasiadoLarga() {
        String larga = "A1$" + "a".repeat(StrongPasswordValidator.MAX_LENGTH);
        assertHasViolation("password", valido().password(larga).build());
    }

    @Test
    @DisplayName("Cualquier símbolo cuenta como carácter especial")
    void simbolosNoAlfanumericosSonValidos() {
        assertTrue(validate(valido().password("Arcash2026/ok").build()).isEmpty());
        assertTrue(validate(valido().password("Arcash2026~ok").build()).isEmpty());
    }

    // -----------------------------------------------------------------
    // Contraseña vs datos de identidad (el caso pedido explícitamente)
    // -----------------------------------------------------------------

    @Test
    @DisplayName("Contraseña igual al nombre de usuario: rechazada")
    void contrasenaIgualAlUsuario() {
        assertHasViolation("password", valido().password("juan.perez").build());
    }

    @Test
    @DisplayName("Contraseña igual al usuario salvo mayúsculas: rechazada")
    void contrasenaIgualAlUsuarioIgnorandoMayusculas() {
        assertHasViolation("password", valido().password("JUAN.PEREZ").build());
    }

    @Test
    @DisplayName("Contraseña que contiene el nombre de usuario: rechazada")
    void contrasenaQueContieneElUsuario() {
        // Es el patrón con el que se esquiva una comprobación de igualdad exacta.
        assertHasViolation("password", valido().password("Juan.perez2026!").build());
    }

    @Test
    @DisplayName("Contraseña igual a la parte local del email: rechazada")
    void contrasenaIgualALaParteLocalDelEmail() {
        assertHasViolation("password",
                valido().alias("otro.alias").password("Juan.perez2026!").build());
    }

    @Test
    @DisplayName("Contraseña que incluye el DNI: rechazada")
    void contrasenaQueIncluyeElDni() {
        assertHasViolation("password", valido().password("Casa30123456$").build());
    }

    // -----------------------------------------------------------------
    // Formato de los demás campos
    // -----------------------------------------------------------------

    @Test
    @DisplayName("DNI: acepta 7 u 8 dígitos y rechaza puntos o letras")
    void formatoDni() {
        assertTrue(validate(valido().dni("1234567").build()).isEmpty());
        assertTrue(validate(valido().dni("12345678").build()).isEmpty());
        assertHasViolation("dni", valido().dni("123456").build());
        assertHasViolation("dni", valido().dni("30.123.456").build());
        assertHasViolation("dni", valido().dni("abcdefgh").build());
    }

    @Test
    @DisplayName("Nombre de usuario: admite puntos internos y rechaza separadores mal ubicados")
    void formatoNombreDeUsuario() {
        assertTrue(validate(valido().alias("juan.perez.01").build()).isEmpty());
        assertTrue(validate(valido().alias("juan_perez").build()).isEmpty());
        assertHasViolation("alias", valido().alias(".juan").build());
        assertHasViolation("alias", valido().alias("juan.").build());
        assertHasViolation("alias", valido().alias("juan..perez").build());
        // Sin al menos una letra se confundiría con un número de cuenta.
        assertHasViolation("alias", valido().alias("12345").build());
        assertHasViolation("alias", valido().alias("ab").build());
    }

    @Test
    @DisplayName("Nombre y apellido: admiten acentos, apóstrofos y guiones")
    void formatoNombreYApellido() {
        assertTrue(validate(valido().lastName("Muñoz").build()).isEmpty());
        assertTrue(validate(valido().lastName("O'Brien").build()).isEmpty());
        assertTrue(validate(valido().lastName("Díaz-López").build()).isEmpty());
        assertTrue(validate(valido().name("Juan Carlos").build()).isEmpty());
        assertHasViolation("name", valido().name(" Juan").build());
        assertHasViolation("name", valido().name("Juan2").build());
        assertHasViolation("name", valido().name("J").build());
    }

    // -----------------------------------------------------------------

    /** Registro base coherente: cada test cambia solo el campo que quiere probar. */
    private RegistrerRequest.RegistrerRequestBuilder valido() {
        return RegistrerRequest.builder()
                .name("Juan")
                .lastName("Pérez")
                .dni("30123456")
                .email("juan.perez@mail.com")
                .alias("juan.perez")
                .password("Arcash2026$seguro");
    }

    private Set<ConstraintViolation<RegistrerRequest>> validate(RegistrerRequest request) {
        return validator.validate(request);
    }

    private void assertHasViolation(String property, RegistrerRequest request) {
        Set<ConstraintViolation<RegistrerRequest>> violations = validate(request);
        assertFalse(violations.isEmpty(), "Se esperaba al menos una violación");

        Set<String> properties = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertTrue(properties.contains(property),
                "Se esperaba una violación en '" + property + "' y llegaron " + properties);
    }

    @Test
    @DisplayName("La violación de identidad se reporta en el campo de la contraseña")
    void laViolacionDeIdentidadApuntaAlCampoPassword() {
        // Esta contraseña cumple toda la política de composición: la única regla que rompe es
        // contener el nombre de usuario, así que queda una sola violación para inspeccionar.
        Set<ConstraintViolation<RegistrerRequest>> violations =
                validate(valido().password("Juan.perez2026!").build());

        assertEquals(1, violations.size());
        // Apuntar al campo y no a la clase permite pintar el error debajo del input correcto.
        assertEquals("password", violations.iterator().next().getPropertyPath().toString());
    }
}
