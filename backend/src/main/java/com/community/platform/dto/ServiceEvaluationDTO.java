package com.community.platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 服务评价请求 DTO
 */
@Data
public class ServiceEvaluationDTO {
    @NotNull(message = "认领记录ID不能为空")
    private Long claimId;
    
    @NotNull(message = "星级不能为空")
    @Min(value = 1, message = "星级最小为1")
    @Max(value = 5, message = "星级最大为5")
    private Byte rating;
    
    /**
     * 评价内容
     */
    private String content;
}
