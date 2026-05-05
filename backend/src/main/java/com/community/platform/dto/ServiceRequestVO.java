package com.community.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 需求列表响应 VO
 */
@Data
public class ServiceRequestVO {
    private Long id;
    private Long requesterUserId;
    private String requesterName;
    private Long communityId;
    private String communityName;
    /** 省（展示用，来自 sys_region） */
    private String province;
    /** 市（展示用，来自 sys_region） */
    private String city;
    private String serviceType;
    private String description;
    private String serviceAddress;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private LocalDateTime expectedTime;
    private Byte urgencyLevel;
    private List<String> specialTags;
    private Byte status;
    private Long auditByUserId;
    private String auditorName;
    private LocalDateTime auditAt;
    private String rejectReason;
    private LocalDateTime publishedAt;
    private LocalDateTime claimedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    /**
     * 最新认领记录（用于移动端“确认完成/评价”联动）
     */
    private Long latestClaimId;
    private Byte latestClaimStatus;
    private String latestVolunteerName;
    /** 志愿者视角的可解释匹配明细 */
    private MatchExplainVO matchExplain;
    /** 推荐理由标签 */
    private List<String> matchReasons;
}
