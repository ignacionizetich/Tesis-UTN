package com.EDJ.ArCash.DTO.AuthDTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VirtualCardRevealResponse {
    private Long id;
    private String currency;
    private String pan;
    private String last4;
    private String cvc;
    private int expMonth;
    private int expYear;
    private String status;
    private double dailyLimit;
    private String holderName;
}
