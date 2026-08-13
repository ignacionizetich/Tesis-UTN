package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.DTO.ApiCalloutDTO.ApiUsdResponse;
import com.EDJ.ArCash.exception.ExchangeRateUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CotizationUsdService {

    private static final Logger log = LoggerFactory.getLogger(CotizationUsdService.class);

    private static final long INTERVALO_ACTUALIZACION_MS = 10 * 60 * 1000;

    private final RestTemplate restTemplate;
    private final String urlProveedor;

    /** La escribe la tarea programada y la leen los hilos que atienden requests. */
    private volatile Double cachedVenta;
    private volatile Double cachedCompra;

    public CotizationUsdService(
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

            if (!actualizoAlgo) {
                log.warn("Respuesta invalida al actualizar la cotizacion del dolar.");
            }
        } catch (Exception e) {
            // Se traga la excepcion a proposito: si ya hay un valor cacheado, el
            // fallo del proveedor no debe tumbar las operaciones en curso.
            log.error("Error al actualizar el precio del dolar: {}", e.getMessage());
        }
    }
}
