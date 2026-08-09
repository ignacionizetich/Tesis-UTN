package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.DTO.AuthDTO.TaxPesosResponse;
import com.EDJ.ArCash.DTO.AuthDTO.TaxUsdResponse;
import com.EDJ.ArCash.exception.ExchangeRateUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests de caracterizacion: describen el comportamiento ACTUAL de TaxService,
 * no el deseado. Sirven de red de seguridad para el refactor posterior.
 */
class TaxServiceTest {

    /** 0.21 y 0.03 no son exactos en binario, asi que las comparaciones van con tolerancia. */
    private static final double DELTA = 1e-9;

    private TaxService taxService;
    private CotizationUsdService cotizationUsdService;

    @BeforeEach
    void setUp() {
        cotizationUsdService = mock(CotizationUsdService.class);
        taxService = new TaxService(cotizationUsdService);
    }

    @Test
    @DisplayName("calcularPesos aplica 21% de IVA y no consulta la cotizacion")
    void calcularPesosAplicaIvaDelVeintiunoPorCiento() {
        TaxPesosResponse resultado = taxService.calcularPesos(10000);

        assertEquals(10000.0, resultado.getMontoOriginal(), DELTA);
        assertEquals("ARS", resultado.getMoneda());
        assertEquals(2100.0, resultado.getIVA(), DELTA);
        assertEquals(12100.0, resultado.getTotalFinal(), DELTA);
        verifyNoInteractions(cotizationUsdService);
    }

    @Test
    @DisplayName("calcularPesos no valida el monto: con un negativo devuelve impuestos negativos")
    void calcularPesosNoValidaMontosNegativos() {
        TaxPesosResponse resultado = taxService.calcularPesos(-100);

        assertEquals(-100.0, resultado.getMontoOriginal(), DELTA);
        assertEquals(-21.0, resultado.getIVA(), DELTA);
        assertEquals(-121.0, resultado.getTotalFinal(), DELTA);
    }

    @Test
    @DisplayName("calcularUSD devuelve montoOriginal ya convertido a pesos pero con moneda USD")
    void calcularUsdDevuelveElMontoEnPesosEtiquetadoComoUsd() {
        when(cotizationUsdService.obtenerCotizacionVenta()).thenReturn(1000.0);

        TaxUsdResponse resultado = taxService.calcularUSD(100);

        // Comportamiento actual conocido: montoOriginal esta en ARS (100 USD * 1000),
        // mientras que moneda dice "USD". El dashboard depende de que venga en pesos.
        assertEquals(100000.0, resultado.getMontoOriginal(), DELTA);
        assertEquals("USD", resultado.getMoneda());
        assertEquals(1000.0, resultado.getPrecioDolar(), DELTA);
        assertEquals(21000.0, resultado.getIVA(), DELTA);
        assertEquals(121000.0, resultado.getTotalFinal(), DELTA);
    }

    @Test
    @DisplayName("calcularUSD propaga la excepcion si no hay cotizacion disponible")
    void calcularUsdPropagaLaExcepcionDeCotizacionNoDisponible() {
        when(cotizationUsdService.obtenerCotizacionVenta())
                .thenThrow(new ExchangeRateUnavailableException("sin cotizacion"));

        assertThrows(ExchangeRateUnavailableException.class, () -> taxService.calcularUSD(100));
    }

    @Test
    @DisplayName("calcularImpuestosConversion devuelve 5 claves con la comision del 3%")
    void calcularImpuestosConversionAplicaComisionDelTresPorCiento() {
        Map<String, Double> resultado = taxService.calcularImpuestosConversion(10000);

        // TransactionService lee estas claves por nombre: renombrarlas rompe las transferencias.
        assertEquals(5, resultado.size());
        assertEquals(0.0, resultado.get("impuestoPais"), DELTA);
        assertEquals(0.0, resultado.get("percepcion"), DELTA);
        assertEquals(300.0, resultado.get("totalImpuestos"), DELTA);
        assertEquals(3.0, resultado.get("porcentajeTotal"), DELTA);
        assertEquals(10300.0, resultado.get("montoConImpuestos"), DELTA);
    }

    @Test
    @DisplayName("calcularImpuestosConversion mantiene porcentajeTotal fijo en 3 aunque el monto sea cero")
    void calcularImpuestosConversionDevuelvePorcentajeFijoConMontoCero() {
        Map<String, Double> resultado = taxService.calcularImpuestosConversion(0);

        assertEquals(0.0, resultado.get("totalImpuestos"), DELTA);
        assertEquals(0.0, resultado.get("montoConImpuestos"), DELTA);
        assertEquals(3.0, resultado.get("porcentajeTotal"), DELTA);
    }
}
