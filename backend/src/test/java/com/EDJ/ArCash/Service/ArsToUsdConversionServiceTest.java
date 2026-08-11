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
 * Clava la formula compartida ARS→USD (misma base numerica que B4/B8).
 */
class ArsToUsdConversionServiceTest {

    private static final double DELTA = 1e-9;

    private CotizationUsdService cotizationUsdService;
    private ArsToUsdConversionService conversionService;

    @BeforeEach
    void setUp() {
        cotizationUsdService = mock(CotizationUsdService.class);
        conversionService = new ArsToUsdConversionService(
                new TaxService(cotizationUsdService),
                cotizationUsdService
        );
    }

    @Test
    @DisplayName("10000 ARS a venta 1000: comision 300, totalDebitado 10300, amountUsd 10 (comision no convertida)")
    void formulaCongeladaComisionNoConvertida() {
        when(cotizationUsdService.obtenerCotizacionVenta()).thenReturn(1000.0);

        ArsToUsdConversion c = conversionService.calculate(10_000.0);

        assertEquals(10_000.0, c.amountArs(), DELTA);
        assertEquals(300.0, c.taxAmount(), DELTA);
        assertEquals(3.0, c.taxPercentage(), DELTA);
        assertEquals(10_300.0, c.totalDebitado(), DELTA);
        assertEquals(1000.0, c.exchangeRate(), DELTA);
        assertEquals(10.0, c.amountUsd(), DELTA);
        // Contraste: si la comision se convirtiera seria 10.3
        assertEquals(10.3, c.totalDebitado() / c.exchangeRate(), DELTA);
    }

    @Test
    @DisplayName("previewDebit no consulta cotizacion")
    void previewDebitNoConsultaCotizacion() {
        DebitPreview preview = conversionService.previewDebit(10_000.0);

        assertEquals(10_000.0, preview.amountArs(), DELTA);
        assertEquals(300.0, preview.taxAmount(), DELTA);
        assertEquals(10_300.0, preview.totalDebitado(), DELTA);
        verify(cotizationUsdService, never()).obtenerCotizacionVenta();
    }
}
