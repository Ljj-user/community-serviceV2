package com.community.platform.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 月度需求对接成功率趋势（按月）
 * successRatePercent = monthlyCompleted / monthlyCreated * 100
 */
@Data
public class MonthlyMatchRateTrendVO {
    /** x 轴标签（如 2026-04） */
    private List<String> labels;
    /** 月度对接成功率（百分比 0-100） */
    private List<BigDecimal> successRatePercent;
    /** 当月新增需求数（分母） */
    private List<Long> createdCount;
    /** 当月完成需求数（分子） */
    private List<Long> completedCount;
}

