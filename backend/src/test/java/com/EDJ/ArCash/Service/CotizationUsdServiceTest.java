package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.DTO.ApiCalloutDTO.ApiUsdResponse;
import com.EDJ.ArCash.exception.ExchangeRateUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CotizationUsdServiceTest {

    private CotizationUsdService cotizationUsdService;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        cotizationUsdService = new CotizationUsdService();
        restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(cotizationUsdService, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("Lanza una excepcion controlada (no NPE) si la API externa falla y no hay cotizacion cacheada")
    void obtenerCotizacionVentaLanzaExcepcionControladaCuandoLaApiFalla() {
        when(restTemplate.getForEntity(anyString(), eq(ApiUsdResponse.class)))
                .thenThrow(new RestClientException("proveedor no disponible"));

        assertThrows(ExchangeRateUnavailableException.class,
                () -> cotizationUsdService.obtenerCotizacionVenta());
    }

    @Test
    @DisplayName("Lanza una excepcion controlada si la API externa responde con venta en cero")
    void obtenerCotizacionVentaLanzaExcepcionControladaCuandoLaVentaEsCero() {
        ApiUsdResponse respuestaInvalida = ApiUsdResponse.builder().venta(0).build();
        when(restTemplate.getForEntity(anyString(), eq(ApiUsdResponse.class)))
                .thenReturn(ResponseEntity.ok(respuestaInvalida));

        assertThrows(ExchangeRateUnavailableException.class,
                () -> cotizationUsdService.obtenerCotizacionVenta());
    }

    @Test
    @DisplayName("Lanza una excepcion controlada si la API externa responde con cuerpo vacio")
    void obtenerCotizacionVentaLanzaExcepcionControladaCuandoElCuerpoEsNulo() {
        when(restTemplate.getForEntity(anyString(), eq(ApiUsdResponse.class)))
                .thenReturn(new ResponseEntity<>((ApiUsdResponse) null, HttpStatus.OK));

        assertThrows(ExchangeRateUnavailableException.class,
                () -> cotizationUsdService.obtenerCotizacionVenta());
    }

    @Test
    @DisplayName("Devuelve la cotizacion de venta y la cachea para las siguientes consultas")
    void obtenerCotizacionVentaDevuelveYCacheaElValor() {
        ApiUsdResponse respuesta = ApiUsdResponse.builder().venta(950.75).build();
        when(restTemplate.getForEntity(anyString(), eq(ApiUsdResponse.class)))
                .thenReturn(ResponseEntity.ok(respuesta));

        assertEquals(950.75, cotizationUsdService.obtenerCotizacionVenta());
        assertEquals(950.75, cotizationUsdService.obtenerCotizacionVenta());

        verify(restTemplate, times(1)).getForEntity(anyString(), eq(ApiUsdResponse.class));
    }

    @Test
    @DisplayName("Sigue devolviendo la cotizacion cacheada si la API externa falla despues")
    void obtenerCotizacionVentaDevuelveElValorCacheadoCuandoLaApiFallaDespues() {
        ApiUsdResponse respuesta = ApiUsdResponse.builder().venta(1200.50).build();
        when(restTemplate.getForEntity(anyString(), eq(ApiUsdResponse.class)))
                .thenReturn(ResponseEntity.ok(respuesta));
        cotizationUsdService.obtenerCotizacionVenta();

        when(restTemplate.getForEntity(anyString(), eq(ApiUsdResponse.class)))
                .thenThrow(new RestClientException("proveedor no disponible"));

        assertEquals(1200.50, cotizationUsdService.obtenerCotizacionVenta());
    }
}
