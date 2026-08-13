package com.EDJ.ArCash.DTO.AuthDTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardAuditEventResponse {
    private Long id;
    private Long cardId;
    private String type;
    private String meta;
    private String createdAt;
}
