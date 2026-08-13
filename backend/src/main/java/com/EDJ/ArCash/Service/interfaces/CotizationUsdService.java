package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.DTO.ApiCalloutDTO.ApiUsdResponse;

public interface CotizationUsdService {
    double obtenerCotizacionVenta();

    double obtenerCotizacionCompra();

    String obtenerFechaActualizacion();

    String obtenerNombreCotizacion();

    String obtenerCasaCotizacion();

    String obtenerMonedaCotizacion();

    ApiUsdResponse obtenerSnapshot();

    void actualizarCotizacion();
}
