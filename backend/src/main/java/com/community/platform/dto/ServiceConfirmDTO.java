package com.community.platform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 需求方核销确认 DTO
 */
@Data
public class ServiceConfirmDTO {

    @NotNull(message = "认领记录ID不能为空")
    private Long claimId;
}

