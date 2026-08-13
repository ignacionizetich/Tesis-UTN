package com.EDJ.ArCash.Service;
import com.EDJ.ArCash.Service.result.UsdDebitPreview;
import com.EDJ.ArCash.Service.result.UsdToArsConversion;
import com.EDJ.ArCash.Service.interfaces.CotizationUsdService;
import com.EDJ.ArCash.Service.interfaces.TaxService;
import com.EDJ.ArCash.Service.interfaces.UsdToArsConversionService;
import com.EDJ.ArCash.Service.impl.TaxServiceImpl;
import com.EDJ.ArCash.Service.impl.UsdToArsConversionServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsdToArsConversionServiceTest {

    private static final double DELTA = 1e-9;

    private CotizationUsdService cotizationUsdService;
    private UsdToArsConversionService conversionService;

    @BeforeEach
    void setUp() {
        cotizationUsdService = mock(CotizationUsdService.class);
        conversionService = new UsdToArsConversionServiceImpl(
                new TaxServiceImpl(cotizationUsdService),
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
