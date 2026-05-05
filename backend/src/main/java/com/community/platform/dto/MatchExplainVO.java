package com.community.platform.dto;

import lombok.Data;

/**
 * 志愿者接单匹配解释
 */
@Data
public class MatchExplainVO {
    private Double totalScore;
    private Double skillScore;
    private Double areaScore;
    private Double priorityScore;
    private Double ratingScore;
    private Double w1 = 0.5D;
    private Double w2 = 0.3D;
    private Double w3 = 0.1D;
    private Double w4 = 0.1D;
}

