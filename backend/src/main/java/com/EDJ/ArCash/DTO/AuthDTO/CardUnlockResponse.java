package com.EDJ.ArCash.DTO.AuthDTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardUnlockResponse {
    private boolean success;
    private String message;
    private String unlockToken;
    private boolean locked;
    private int expiresInSeconds;
}
