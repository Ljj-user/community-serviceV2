package com.community.platform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 需求审核请求 DTO
 */
@Data
public class ServiceRequestAuditDTO {
    @NotNull(message = "需求ID不能为空")
    private Long requestId;
    
    /**
     * 审核结果：true通过 false驳回
     */
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;
    
    /**
     * 驳回原因（当approved=false时必填）
     */
    private String rejectReason;
}
