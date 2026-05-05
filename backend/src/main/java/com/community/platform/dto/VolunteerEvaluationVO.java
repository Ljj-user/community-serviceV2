package com.community.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 志愿者收到的评价 VO
 */
@Data
public class VolunteerEvaluationVO {
    private Long id;
    private Long claimId;
    private Long requestId;
    private String serviceType;
    private String serviceAddress;

    private Long residentUserId;
    private String residentName;

    private Byte rating;
    private String content;
    private LocalDateTime createdAt;
}

