package com.community.platform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 完成服务请求 DTO
 */
@Data
public class ServiceCompleteDTO {
    @NotNull(message = "认领记录ID不能为空")
    private Long claimId;
    
    @NotNull(message = "服务时长不能为空")
    @Positive(message = "服务时长必须大于0")
    private BigDecimal serviceHours;
    
    /**
     * 完成说明/备注
     */
    private String completionNote;
}
