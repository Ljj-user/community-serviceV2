package com.community.platform.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiOrderDraftVO {
    private String serviceType;
    private Integer urgencyLevel;
    private String expectedTime;
    private List<String> tags;
    private String description;
}
