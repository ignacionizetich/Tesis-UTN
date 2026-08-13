package com.EDJ.ArCash.Service.impl;
import com.EDJ.ArCash.Service.interfaces.CotizationUsdService;

import com.EDJ.ArCash.DTO.ApiCalloutDTO.ApiUsdResponse;
import com.EDJ.ArCash.exception.personalizated.ExchangeRateUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CotizationUsdServiceImpl implements CotizationUsdService {

    private static final Logger log = LoggerFactory.getLogger(CotizationUsdService.class);

    private static final long INTERVALO_ACTUALIZACION_MS = 10 * 60 * 1000;

    private final RestTemplate restTemplate;
    private final String urlProveedor;

    /** La escribe la tarea programada y la leen los hilos que atienden requests. */
    private volatile Double cachedVenta;
    private volatile Double cachedCompra;
    private volatile String cachedFechaActualizacion;
    private volatile String cachedNombre;
    private volatile String cachedCasa;
    private volatile String cachedMoneda;

    public CotizationUsdServiceImpl(
            RestTemplate restTemplate,
            @Value("${app.dolar.api.url}") String urlProveedor) {
        this.restTemplate = restTemplate;
        this.urlProveedor = urlProveedor;
    }

    public double obtenerCotizacionVenta() {
        Double cotizacion = cachedVenta;

        if (cotizacion == null) {
            actualizarCotizacion();
            cotizacion = cachedVenta;
        }

        if (cotizacion == null) {
            throw new ExchangeRateUnavailableException(
                    "No se pudo obtener la cotizacion del dolar desde el proveedor externo.");
        }

        return cotizacion;
    }

    public double obtenerCotizacionCompra() {
        Double cotizacion = cachedCompra;

        if (cotizacion == null) {
            actualizarCotizacion();
            cotizacion = cachedCompra;
        }

        if (cotizacion == null) {
            throw new ExchangeRateUnavailableException(
                    "No se pudo obtener la cotizacion de compra del dolar desde el proveedor externo.");
        }

        return cotizacion;
    }

    /** Metadatos de dolarapi.com (pueden ser null si aún no hay cache). */
    public String obtenerFechaActualizacion() {
        return cachedFechaActualizacion;
    }

    public String obtenerNombreCotizacion() {
        return cachedNombre;
    }

    public String obtenerCasaCotizacion() {
        return cachedCasa;
    }

    public String obtenerMonedaCotizacion() {
        return cachedMoneda;
    }

    public ApiUsdResponse obtenerSnapshot() {
        if (cachedVenta == null) {
            actualizarCotizacion();
        }
        if (cachedVenta == null) {
            throw new ExchangeRateUnavailableException(
                    "No se pudo obtener la cotizacion del dolar desde el proveedor externo.");
        }
        return ApiUsdResponse.builder()
                .moneda(cachedMoneda != null ? cachedMoneda : "USD")
                .casa(cachedCasa)
                .nombre(cachedNombre)
                .compra(cachedCompra != null ? cachedCompra : 0)
                .venta(cachedVenta)
                .fechaActualizacion(cachedFechaActualizacion)
                .build();
    }

    @Scheduled(fixedRate = INTERVALO_ACTUALIZACION_MS)
    public void actualizarCotizacion() {
        try {
            ResponseEntity<ApiUsdResponse> response =
                    restTemplate.getForEntity(urlProveedor, ApiUsdResponse.class);
            ApiUsdResponse cotizacion = response.getBody();

            if (cotizacion == null) {
                log.warn("Respuesta invalida al actualizar la cotizacion del dolar.");
                return;
            }

            boolean actualizoAlgo = false;
            if (cotizacion.getVenta() > 0) {
                cachedVenta = cotizacion.getVenta();
                actualizoAlgo = true;
            }
            if (cotizacion.getCompra() > 0) {
                cachedCompra = cotizacion.getCompra();
                actualizoAlgo = true;
            }

            if (actualizoAlgo) {
                cachedFechaActualizacion = cotizacion.getFechaActualizacion();
                cachedNombre = cotizacion.getNombre();
                cachedCasa = cotizacion.getCasa();
                cachedMoneda = cotizacion.getMoneda();
            } else {
                log.warn("Respuesta invalida al actualizar la cotizacion del dolar.");
            }
        } catch (Exception e) {
            // Se traga la excepcion a proposito: si ya hay un valor cacheado, el
            // fallo del proveedor no debe tumbar las operaciones en curso.
            log.error("Error al actualizar el precio del dolar: {}", e.getMessage());
        }
    }
}
