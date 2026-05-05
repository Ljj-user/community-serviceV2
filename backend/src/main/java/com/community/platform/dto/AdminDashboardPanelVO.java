package com.community.platform.dto;

import lombok.Data;

import java.util.List;

/**
 * 管理员首页看板（社区管理员 / 超级管理员）
 */
@Data
public class AdminDashboardPanelVO {
    /** SUPER_ADMIN 或 COMMUNITY_ADMIN */
    private String scope;
    private DashboardStatsVO stats;
    private List<RegionStatVO> regionCoverage;
    private List<NameCountVO> demandByServiceType;
    /** 仅超级管理员：资金/物资占位监控 */
    private FundingMonitorVO fundingMonitor;
    /** 近期服务排期（期望时间在未来） */
    private List<ScheduleBriefVO> upcomingSchedule;
}
