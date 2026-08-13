package com.EDJ.ArCash.DTO.AuthDTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardLimitRequest {
    private double dailyLimit;
}
