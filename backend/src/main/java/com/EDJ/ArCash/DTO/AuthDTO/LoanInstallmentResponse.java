package com.EDJ.ArCash.DTO.AuthDTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanInstallmentResponse {
    private Long id;
    private int number;
    private String dueDate;
    private double amount;
    private String status;
    private String paidAt;
}
