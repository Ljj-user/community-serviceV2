package com.community.platform.dto;

import lombok.Data;

import java.util.List;

/**
 * 供需趋势对比（折线图）数据
 */
@Data
public class TrendChartVO {
    /** x 轴标签（如 04-16） */
    private List<String> labels;
    /** 需求（发布/新建） */
    private List<Long> demand;
    /** 供给（认领/完成） */
    private List<Long> supply;
}

