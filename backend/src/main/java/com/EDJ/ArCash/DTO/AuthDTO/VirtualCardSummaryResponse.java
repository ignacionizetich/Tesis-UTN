package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resumen de prepaga virtual (sin datos sensibles)")
public class VirtualCardSummaryResponse {
    private Long id;
    private Long accountId;
    private String currency;
    private String last4;
    private String status;
    private double dailyLimit;
    private String holderName;
    private boolean pinConfigured;
    private int expMonth;
    private int expYear;
    private boolean expired;
    private String cancelledAt;
    private boolean canReissue;
    /** Mensaje corto si aún no se puede solicitar otra (cooldown / sigue vigente). */
    private String reissueMessage;
}
