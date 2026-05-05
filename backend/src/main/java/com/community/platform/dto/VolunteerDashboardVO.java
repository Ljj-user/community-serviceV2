package com.community.platform.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 志愿者个人看板
 */
@Data
public class VolunteerDashboardVO {
    private BigDecimal totalServiceHours;
    private Long pendingClaimCount;
    private Long completedClaimCount;
    /** 居民对我评价的平均分（无则 null） */
    private BigDecimal averageRating;
    private Long evaluationCount;
    /** 志愿信用分（累计） */
    private BigDecimal creditScore;
    /** 近30天平均评分 */
    private BigDecimal avgRating30d;
    /** 近30天履约率 */
    private BigDecimal completionRate30d;
    /** 最近参与的需求标题 */
    private List<String> recentProjectTitles;
    /** 荣誉/排名占位说明 */
    private String honorNote;
}
