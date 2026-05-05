package com.community.platform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 服务认领请求 DTO
 */
@Data
public class ServiceClaimDTO {
    @NotNull(message = "需求ID不能为空")
    private Long requestId;
}
