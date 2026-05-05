package com.community.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 已评价记录 VO（居民侧）
 */
@Data
public class ServiceEvaluationHistoryVO {
    private Long id;
    private Long claimId;
    private Long requestId;
    private String serviceType;
    private String serviceAddress;
    private Long volunteerUserId;
    private String volunteerName;
    private Byte rating;
    private String content;
    private LocalDateTime createdAt;
}

