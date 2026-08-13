package com.EDJ.ArCash.DTO.AuthDTO;

import java.util.List;

public record LoanRatesResponse(List<LoanRateItem> rates, String updatedAt) {
    public record LoanRateItem(
            int installments,
            double monthlyRate,
            double monthlyRatePercent
    ) {}
}
