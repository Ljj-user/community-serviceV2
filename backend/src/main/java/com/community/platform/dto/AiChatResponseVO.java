package com.community.platform.dto;

import lombok.Data;

@Data
public class AiChatResponseVO {
    /**
     * DEMAND_DRAFT / FAQ
     */
    private String mode;
    private String reply;
    private Long analysisRecordId;
    private AiOrderDraftVO orderDraft;
}
