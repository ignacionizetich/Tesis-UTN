package com.EDJ.ArCash.Controller.api;

import com.EDJ.ArCash.DTO.TaxPesosRequest;
import com.EDJ.ArCash.DTO.TaxPesosResponse;
import com.EDJ.ArCash.DTO.TaxUsdRequest;
import com.EDJ.ArCash.DTO.TaxUsdResponse;
import com.EDJ.ArCash.Service.TaxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/impuestos", produces = "application/json")
public class TaxController {
    @Autowired
    private TaxService taxService;


    @GetMapping("/calculateARS")
    public ResponseEntity<?> calcularARS(@RequestParam double montoARS) {
        if (montoARS <= 0) {
            return ResponseEntity.badRequest().body("El monto en ARS no puede ser cero o negativo.");
        }
        TaxPesosResponse resultado = taxService.calcularPesos(montoARS);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/calculateUSD")
    public ResponseEntity<?> calcularUSD(@RequestParam double montoUSD) {
        if (montoUSD <= 0) {
            return ResponseEntity.badRequest().body("El monto en USD no puede ser cero.");
        }

        TaxUsdResponse taxUsdResponse = taxService.calcularUSD(montoUSD);
        return ResponseEntity.ok(taxUsdResponse);
    }
}
