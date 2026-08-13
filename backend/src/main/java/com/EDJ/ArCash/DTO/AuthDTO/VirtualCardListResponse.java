package com.EDJ.ArCash.DTO.AuthDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VirtualCardListResponse {
    private boolean pinConfigured;
    private List<VirtualCardSummaryResponse> cards;
}
