package com.community.platform.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 待评价服务 VO（居民侧）
 */
@Data
public class ServiceEvaluationPendingVO {
    private Long claimId;
    private Long requestId;
    private String serviceType;
    private String serviceAddress;
    private LocalDateTime completedAt;
    private BigDecimal serviceHours;
    private Long volunteerUserId;
    private String volunteerName;
}

