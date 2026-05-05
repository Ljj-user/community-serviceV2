package com.community.platform.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 大厅-服务概览汇总数据
 */
@Data
public class HallSummaryVO {
    /** 我的发布需求数量 */
    private Long myPublishedCount;
    /** 我作为志愿者已完成的任务数（认领状态=已完成） */
    private Long myCompletedCount;
    /** 进行中的单子数量（已认领未完成） */
    private Long inProgressCount;
    /** 我收到的评价数量 */
    private Long receivedEvaluationCount;
    /** 我收到的平均评分（1-5），无数据时为 null */
    private BigDecimal receivedAvgRating;
}

