package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.ApiCalloutDTO.ApiUsdResponse;
import com.EDJ.ArCash.Service.interfaces.CotizationUsdService;
import com.EDJ.ArCash.exception.personalizated.ExchangeRateUnavailableException;
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
class CotizacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CotizationUsdService cotizationUsdService;

    @Test
    @WithMockUser
    @DisplayName("GET /api/cotizacion/dolar devuelve compra, venta y metadatos")
    void obtenerDolarOficialDevuelveSnapshot() throws Exception {
        when(cotizationUsdService.obtenerSnapshot()).thenReturn(
                ApiUsdResponse.builder()
                        .moneda("USD")
                        .casa("oficial")
                        .nombre("Oficial")
                        .compra(1465.0)
                        .venta(1515.0)
                        .fechaActualizacion("2026-08-13T16:00:00.000Z")
                        .build()
        );

        mockMvc.perform(get("/api/cotizacion/dolar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moneda").value("USD"))
                .andExpect(jsonPath("$.casa").value("oficial"))
                .andExpect(jsonPath("$.nombre").value("Oficial"))
                .andExpect(jsonPath("$.compra").value(1465.0))
                .andExpect(jsonPath("$.venta").value(1515.0))
                .andExpect(jsonPath("$.fechaActualizacion").value("2026-08-13T16:00:00.000Z"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/cotizacion/dolar responde 503 si no hay cotización")
    void obtenerDolarOficialSinCotizacionDevuelve503() throws Exception {
        when(cotizationUsdService.obtenerSnapshot())
                .thenThrow(new ExchangeRateUnavailableException("sin cotizacion"));

        mockMvc.perform(get("/api/cotizacion/dolar"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("sin cotizacion"));
    }

    @Test
    @DisplayName("GET /api/cotizacion/dolar sin auth responde 401")
    void obtenerDolarOficialSinAuthDevuelve401() throws Exception {
        mockMvc.perform(get("/api/cotizacion/dolar"))
                .andExpect(status().isUnauthorized());
    }
}
