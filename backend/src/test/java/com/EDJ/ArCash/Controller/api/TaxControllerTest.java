package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.Service.interfaces.CotizationUsdService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaxControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CotizationUsdService cotizationUsdService;

    @Test
    @WithMockUser
    @DisplayName("calculateARS devuelve 200 y publica el IVA bajo la clave 'iva' en minuscula")
    void calculateArsDevuelveElCalculoConLaClaveIvaEnMinuscula() throws Exception {
        mockMvc.perform(get("/api/impuestos/calculateARS").param("montoARS", "10000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoOriginal").value(10000.0))
                .andExpect(jsonPath("$.moneda").value("ARS"))
                .andExpect(jsonPath("$.alicuotaIva").value(21.0))
                .andExpect(jsonPath("$.iva").value(2100.0))
                .andExpect(jsonPath("$.totalFinal").value(12100.0))
                // data-service.ts lee response.iva: si el campo se serializa como "IVA" el front rompe.
                .andExpect(jsonPath("$.IVA").doesNotExist());
    }

    @Test
    @WithMockUser
    @DisplayName("calculateARS rechaza el monto cero con 400 y mensaje propio")
    void calculateArsRechazaMontoCero() throws Exception {
        mockMvc.perform(get("/api/impuestos/calculateARS").param("montoARS", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El monto en ARS no puede ser cero o negativo."));
    }

    @Test
    @WithMockUser
    @DisplayName("calculateARS rechaza montos negativos con el mismo mensaje que el cero")
    void calculateArsRechazaMontoNegativo() throws Exception {
        mockMvc.perform(get("/api/impuestos/calculateARS").param("montoARS", "-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El monto en ARS no puede ser cero o negativo."));
    }

    @Test
    @WithMockUser
    @DisplayName("calculateUSD devuelve montoOriginal en pesos con moneda USD")
    void calculateUsdDevuelveElMontoConvertidoAPesos() throws Exception {
        when(cotizationUsdService.obtenerCotizacionVenta()).thenReturn(1000.0);
        when(cotizationUsdService.obtenerCotizacionCompra()).thenReturn(950.0);
        when(cotizationUsdService.obtenerNombreCotizacion()).thenReturn("Oficial");
        when(cotizationUsdService.obtenerCasaCotizacion()).thenReturn("oficial");
        when(cotizationUsdService.obtenerFechaActualizacion()).thenReturn("2026-08-13T16:00:00.000Z");

        mockMvc.perform(get("/api/impuestos/calculateUSD").param("montoUSD", "100"))
                .andExpect(status().isOk())
                // El dashboard rotula montoOriginal como "Monto en ARS": debe seguir viniendo convertido.
                .andExpect(jsonPath("$.montoUsd").value(100.0))
                .andExpect(jsonPath("$.montoOriginal").value(100000.0))
                .andExpect(jsonPath("$.moneda").value("USD"))
                .andExpect(jsonPath("$.precioDolar").value(1000.0))
                .andExpect(jsonPath("$.dolarVenta").value(1000.0))
                .andExpect(jsonPath("$.dolarCompra").value(950.0))
                .andExpect(jsonPath("$.nombreCotizacion").value("Oficial"))
                .andExpect(jsonPath("$.casa").value("oficial"))
                .andExpect(jsonPath("$.fechaActualizacion").value("2026-08-13T16:00:00.000Z"))
                .andExpect(jsonPath("$.alicuotaIva").value(21.0))
                .andExpect(jsonPath("$.iva").value(21000.0))
                .andExpect(jsonPath("$.totalFinal").value(121000.0));
    }

    @Test
    @WithMockUser
    @DisplayName("calculateUSD rechaza el monto cero con 400 y mensaje propio")
    void calculateUsdRechazaMontoCero() throws Exception {
        mockMvc.perform(get("/api/impuestos/calculateUSD").param("montoUSD", "0"))
                .andExpect(status().isBadRequest())
                // El texto no menciona negativos aunque el if tambien los rechaza.
                .andExpect(jsonPath("$.error").value("El monto en USD no puede ser cero."));
    }

    @Test
    @DisplayName("calculateARS sin autenticacion devuelve 401")
    void calculateArsSinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(get("/api/impuestos/calculateARS").param("montoARS", "10000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("calculateARS sin el parametro montoARS devuelve 500, no 400")
    void calculateArsSinParametroDevuelve500() throws Exception {
        // El @ExceptionHandler(Exception.class) de GlobalExceptionHandler atrapa la
        // MissingServletRequestParameterException antes de que Spring la resuelva como 400,
        // y ademas el cuerpo no tiene la misma forma que el 400 manual del controller.
        mockMvc.perform(get("/api/impuestos/calculateARS"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Error interno del servidor"))
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/impuestos/calculateARS"));
    }
}
