package com.EDJ.ArCash.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Clava la formula USD→ARS (comision en USD, credito ARS solo sobre la base).
 */
class UsdToArsConversionServiceTest {

    private static final double DELTA = 1e-9;

    private CotizationUsdService cotizationUsdService;
    private UsdToArsConversionService conversionService;

    @BeforeEach
    void setUp() {
        cotizationUsdService = mock(CotizationUsdService.class);
        conversionService = new UsdToArsConversionService(
                new TaxService(cotizationUsdService),
                cotizationUsdService
        );
    }

    @Test
    @DisplayName("100 USD a compra 1000: comision 3, totalDebitado 103, amountArs 100000 (comision no convertida)")
    void formulaCongeladaComisionNoConvertida() {
        when(cotizationUsdService.obtenerCotizacionCompra()).thenReturn(1000.0);

        UsdToArsConversion c = conversionService.calculate(100.0);

        assertEquals(100.0, c.amountUsd(), DELTA);
        assertEquals(3.0, c.taxAmount(), DELTA);
        assertEquals(3.0, c.taxPercentage(), DELTA);
        assertEquals(103.0, c.totalDebitado(), DELTA);
        assertEquals(1000.0, c.exchangeRate(), DELTA);
        assertEquals(100_000.0, c.amountArs(), DELTA);
        // Contraste: si la comision se convirtiera seria 103000
        assertEquals(103_000.0, c.totalDebitado() * c.exchangeRate(), DELTA);
    }

    @Test
    @DisplayName("previewDebit no consulta cotizacion")
    void previewDebitNoConsultaCotizacion() {
        UsdDebitPreview preview = conversionService.previewDebit(100.0);

        assertEquals(100.0, preview.amountUsd(), DELTA);
        assertEquals(3.0, preview.taxAmount(), DELTA);
        assertEquals(103.0, preview.totalDebitado(), DELTA);
        verify(cotizationUsdService, never()).obtenerCotizacionCompra();
        verify(cotizationUsdService, never()).obtenerCotizacionVenta();
    }
}
