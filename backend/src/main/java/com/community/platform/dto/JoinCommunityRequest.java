package com.community.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinCommunityRequest {
    @NotBlank(message = "邀请码不能为空")
    private String code;
}

