package com.community.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务过程监控 VO
 */
@Data
public class ServiceMonitorVO {

    /**
     * 需求ID
     */
    private Long requestId;

    /**
     * 需求状态
     */
    private Byte status;

    /**
     * 风险类型：1超时未认领 2超时未完成
     */
    private Integer riskType;

    /**
     * 告警来源编码：TIMEOUT_UNCLAIMED / TIMEOUT_INCOMPLETE / CARE_INACTIVE / DEMAND_SURGE
     */
    private String alertSource;

    /**
     * 触发规则摘要
     */
    private String triggerRule;

    /**
     * 建议动作
     */
    private String suggestionAction;

    /**
     * 服务类型
     */
    private String serviceType;
    private Long communityId;
    private String communityName;

    /**
     * 服务地址
     */
    private String serviceAddress;

    /**
     * 期望服务时间
     */
    private LocalDateTime expectedTime;

    /**
     * 紧急程度
     */
    private Byte urgencyLevel;

    /**
     * 需求发起人姓名
     */
    private String requesterName;

    /**
     * 认领志愿者姓名
     */
    private String volunteerName;

    /**
     * 认领记录ID
     */
    private Long claimId;

    /**
     * 认领状态
     */
    private Byte claimStatus;

    /**
     * 超时时长（分钟）
     */
    private Long overtimeMinutes;

    /**
     * 评价星级（如有）
     */
    private Byte rating;

    private LocalDateTime claimedAt;

    private LocalDateTime completedAt;
}

