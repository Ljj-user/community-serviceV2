package com.community.platform.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserOnboardingSubmitRequest {
    @Size(max = 10, message = "最多选择10项技能")
    private List<String> skillTags;
    @Size(max = 10, message = "最多选择10项关注功能")
    private List<String> preferredFeatures;
    @Size(max = 500, message = "备注不超过500字")
    private String intentNote;
}

