package com.EDJ.ArCash.DTO.AuthDTO;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanSimulationResponse {
    private double principal;
    private int installments;
    private double monthlyRate;
    private double monthlyRatePercent;
    private double installmentAmount;
    private double totalAmount;
    private double totalInterest;
    private List<LoanInstallmentResponse> schedule;
}
