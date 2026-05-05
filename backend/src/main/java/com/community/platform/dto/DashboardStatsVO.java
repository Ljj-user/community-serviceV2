package com.community.platform.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 数据看板统计 VO
 */
@Data
public class DashboardStatsVO {
    /** 平台用户总数（未删除） */
    private Long totalUsers;
    /** 入驻社区数（sys_region level=3） */
    private Long totalCommunities;

    /**
     * 总需求数
     */
    private Long totalRequests;
    
    /**
     * 待审核需求数
     */
    private Long pendingRequests;
    
    /**
     * 已发布需求数
     */
    private Long publishedRequests;
    
    /**
     * 已完成需求数
     */
    private Long completedRequests;

    /**
     * 已认领需求数
     */
    private Long claimedRequests;

    /**
     * 待确认需求数（志愿者已提交完成，等待需求方确认）
     */
    private Long pendingConfirmRequests;

    /**
     * 已驳回需求数
     */
    private Long rejectedRequests;
    
    /**
     * 总服务时长（小时）
     */
    private BigDecimal totalServiceHours;
    
    /**
     * 活跃志愿者数
     */
    private Long activeVolunteers;
    
    /**
     * 本月新增需求数
     */
    private Long monthlyNewRequests;
    
    /**
     * 本月完成需求数
     */
    private Long monthlyCompletedRequests;

    /**
     * 需求对接率（completedRequests / totalRequests，0-1 之间的小数）
     */
    private BigDecimal matchRate;

    /**
     * 服务覆盖率（活跃志愿者数 / 有效志愿者总数，0-1 之间的小数）
     */
    private BigDecimal coverageRate;

    /** 系统风险指数（0-100，越高风险越大） */
    private BigDecimal riskIndex;

    /** 本周响应时效（分钟）：需求发布->志愿者认领的平均时长 */
    private BigDecimal weeklyAvgResponseMinutes;

    /** 近30天活跃志愿者储备（有认领记录的志愿者人数） */
    private Long activeVolunteers30d;
}
