package com.community.platform.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务认领记录 VO
 */
@Data
public class ServiceClaimVO {
    private Long id;
    private Long requestId;
    private String requestTitle;  // 需求标题（服务类型）
    private String requestAddress;  // 服务地址
    private Long volunteerUserId;
    private String volunteerName;
    private String requesterName;
    private String requesterPhone;
    private LocalDateTime claimAt;
    private Byte claimStatus;
    private BigDecimal serviceHours;
    private LocalDateTime hoursSubmittedAt;
    private String completionNote;
    private LocalDateTime createdAt;
}
