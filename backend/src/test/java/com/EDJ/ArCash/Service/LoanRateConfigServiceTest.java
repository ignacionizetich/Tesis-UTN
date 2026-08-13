package com.EDJ.ArCash.Service;

import com.EDJ.ArCash.Service.interfaces.LoanRateConfigService;
import com.EDJ.ArCash.Service.interfaces.LoanService;
import com.EDJ.ArCash.DTO.AuthDTO.LoanRatesUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LoanRateConfigServiceTest {

    @Autowired
    private LoanRateConfigService loanRateConfigService;

    @Autowired
    private LoanService loanService;

    @Test
    @DisplayName("Seed crea tasas por defecto")
    void seedDefaults() {
        var rates = loanRateConfigService.listRates();
        assertEquals(3, rates.rates().size());
        assertEquals(3.0, rates.rates().get(0).monthlyRatePercent(), 0.01);
    }

    @Test
    @DisplayName("Actualizar tasa impacta la simulación")
    void updateImpactaSimulacion() {
        LoanRatesUpdateRequest request = new LoanRatesUpdateRequest();
        LoanRatesUpdateRequest.LoanRateUpdateItem item = new LoanRatesUpdateRequest.LoanRateUpdateItem();
        item.setInstallments(3);
        item.setMonthlyRatePercent(6.0);
        request.setRates(List.of(item));

        loanRateConfigService.updateRates(request);

        assertEquals(0.06, loanRateConfigService.monthlyRateFor(3), 0.0001);
        var sim = loanService.simulate(12_000, 3);
        assertEquals(0.06, sim.monthlyRate(), 0.0001);

        // Restaurar default para no contaminar otros tests del contexto Spring.
        item.setMonthlyRatePercent(3.0);
        loanRateConfigService.updateRates(request);
    }

    @Test
    @DisplayName("Rechaza tasa fuera de rango")
    void rechazaFueraDeRango() {
        LoanRatesUpdateRequest request = new LoanRatesUpdateRequest();
        LoanRatesUpdateRequest.LoanRateUpdateItem item = new LoanRatesUpdateRequest.LoanRateUpdateItem();
        item.setInstallments(6);
        item.setMonthlyRatePercent(50.0);
        request.setRates(List.of(item));

        assertThrows(IllegalArgumentException.class, () -> loanRateConfigService.updateRates(request));
    }
}
