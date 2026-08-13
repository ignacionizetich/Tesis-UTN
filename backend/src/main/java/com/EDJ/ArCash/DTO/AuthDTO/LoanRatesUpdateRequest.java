package com.EDJ.ArCash.DTO.AuthDTO;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class LoanRatesUpdateRequest {
    private List<LoanRateUpdateItem> rates;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LoanRateUpdateItem {
        private int installments;
        /** Porcentaje mensual (ej. 4.0 = 4%). */
        private double monthlyRatePercent;
    }
}
