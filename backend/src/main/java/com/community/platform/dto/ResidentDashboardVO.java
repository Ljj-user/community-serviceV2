package com.community.platform.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 居民个人看板
 */
@Data
public class ResidentDashboardVO {
    /** 状态 -> 数量：0待审核 1已发布 2已认领 3已完成 4已驳回 */
    private Map<String, Long> requestStatusCounts;
    /** 我给出的评价条数 */
    private Long evaluationsGivenCount;
    private List<AnnouncementBriefVO> latestAnnouncements;
}
