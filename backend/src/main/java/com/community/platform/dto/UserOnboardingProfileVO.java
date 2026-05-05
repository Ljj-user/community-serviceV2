package com.community.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserOnboardingProfileVO {
    private Long userId;
    private List<String> skillTags;
    private List<String> preferredFeatures;
    private String intentNote;
    private LocalDateTime updatedAt;
}

