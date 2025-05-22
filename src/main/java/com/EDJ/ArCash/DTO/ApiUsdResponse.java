package com.EDJ.ArCash.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiUsdResponse {
    private String moneda;
    private String casa;
    private String nombre;
    private double compra;
    private double venta;
    private String fechaActualizacion;
}
