package com.community.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServiceDisputeDTO {

    @NotNull(message = "认领记录ID不能为空")
    private Long claimId;

    @NotBlank(message = "申诉内容不能为空")
    private String reason;
}
