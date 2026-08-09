package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Congela el formato de los identificadores que se le asignan a una cuenta.
 * Es lo mas delicado de todo el modulo: son datos que quedan persistidos y que
 * los usuarios comparten para recibir transferencias.
 */
class AccountIdentifierGeneratorTest {

    private AccountRepository accountRepository;
    private AccountIdentifierGenerator generator;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        generator = new AccountIdentifierGenerator(accountRepository);
    }

    @Test
    @DisplayName("El CVU tiene 22 digitos y arranca con el codigo de entidad")
    void elCvuTieneElFormatoEsperado() {
        String cvu = generator.generateUniqueCvu();

        assertEquals(22, cvu.length());
        assertTrue(cvu.startsWith("00002001"), "el CVU deberia arrancar con el codigo de entidad: " + cvu);
        assertTrue(cvu.matches("\\d{22}"), "el CVU deberia ser solo digitos: " + cvu);
    }

    @Test
    @DisplayName("El alias son dos palabras y dos letras, en mayusculas")
    void elAliasTieneElFormatoEsperado() {
        String alias = generator.generateUniqueNickname();

        assertTrue(alias.matches("[A-Z]+\\.[A-Z]+\\.[A-Z]{2}"),
                "el alias generado no tiene el formato esperado: " + alias);
        assertTrue(alias.length() <= 25);
    }

    @Test
    @DisplayName("Reintenta mientras el alias ya exista")
    void reintentaSiElAliasYaExiste() {
        when(accountRepository.existsByAccountNickname(anyString())).thenReturn(true, true, false);

        generator.generateUniqueNickname();

        verify(accountRepository, times(3)).existsByAccountNickname(anyString());
    }

    @Test
    @DisplayName("Reintenta mientras el CVU ya exista")
    void reintentaSiElCvuYaExiste() {
        when(accountRepository.existsByAccountCvu(anyString())).thenReturn(true, false);

        generator.generateUniqueCvu();

        verify(accountRepository, times(2)).existsByAccountCvu(anyString());
    }
}
