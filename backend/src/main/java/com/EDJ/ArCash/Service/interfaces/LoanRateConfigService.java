package com.EDJ.ArCash.Service.interfaces;

import com.EDJ.ArCash.DTO.AuthDTO.LoanRatesResponse;
import com.EDJ.ArCash.DTO.AuthDTO.LoanRatesUpdateRequest;
import java.util.Set;

public interface LoanRateConfigService {
    Set<Integer> ALLOWED_INSTALLMENTS = Set.of(3, 6, 12);
    double MIN_RATE_PERCENT = 0.5;
    double MAX_RATE_PERCENT = 15.0;

    void seedDefaults();

    double monthlyRateFor(int installments);

    LoanRatesResponse listRates();

    LoanRatesResponse updateRates(LoanRatesUpdateRequest request);
}
