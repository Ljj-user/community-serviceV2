package com.community.platform.service;

import com.community.platform.dto.AdminDashboardPanelVO;
import com.community.platform.dto.DashboardStatsVO;
import com.community.platform.dto.FundingMonitorVO;
import com.community.platform.dto.MonthlyMatchRateTrendVO;
import com.community.platform.dto.NameCountVO;
import com.community.platform.dto.RegionStatVO;
import com.community.platform.dto.ScheduleBriefVO;
import com.community.platform.dto.TrendChartVO;

import java.util.List;

/**
 * 数据看板服务接口
 */
public interface DashboardService {

    /**
     * 获取统计数据（管理员）
     */
    DashboardStatsVO getStats(Long communityId);

    /**
     * 按区域聚合的服务覆盖统计（用于热力图）
     */
    List<RegionStatVO> getRegionCoverage(Long communityId);

    /**
     * 按服务类型统计需求分布
     */
    List<NameCountVO> getDemandByServiceType(Long communityId);

    /**
     * 资金/物资监控占位（超级管理员）
     */
    FundingMonitorVO getFundingMonitorPlaceholder();

    /**
     * 近期期望服务时间的需求排期
     */
    List<ScheduleBriefVO> getUpcomingSchedule(int limit, Long communityId);

    /**
     * 管理员首页看板聚合（根据是否超管附带资金监控）
     */
    AdminDashboardPanelVO buildAdminPanel(boolean superAdmin, Long communityId);

    /**
     * 供需趋势对比（近 N 天）
     */
    TrendChartVO getSupplyDemandTrend(Long communityId, int days);

    /**
     * 志愿者荣誉榜 TopN（按完成时长）
     */
    List<NameCountVO> getVolunteerHonorTop(Long communityId, int days, int topN);

    /**
     * 各社区服务量对比 TopN（按已完成需求数）
     */
    List<NameCountVO> getCommunityServiceTop(Long communityId, int topN);

    /**
     * 月度需求对接成功率趋势（近 N 个月）
     */
    MonthlyMatchRateTrendVO getMonthlyMatchRateTrend(Long communityId, int months);
}
