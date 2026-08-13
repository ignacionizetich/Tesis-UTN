package com.EDJ.ArCash.DTO.AuthDTO;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanDetailResponse {
    private Long id;
    private double principal;
    private int installmentCount;
    private double installmentAmount;
    private double totalAmount;
    private double totalInterest;
    private double monthlyRatePercent;
    private String status;
    private String createdAt;
    private List<LoanInstallmentResponse> installments;
}
