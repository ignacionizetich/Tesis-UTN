package com.EDJ.ArCash.DTO.AuthDTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanSummaryResponse {
    private Long id;
    private double principal;
    private int installmentCount;
    private double installmentAmount;
    private double totalAmount;
    private double totalInterest;
    private double monthlyRatePercent;
    private String status;
    private String createdAt;
    private int paidCount;
    private int pendingCount;
    private Double nextInstallmentAmount;
    private String nextDueDate;
}
