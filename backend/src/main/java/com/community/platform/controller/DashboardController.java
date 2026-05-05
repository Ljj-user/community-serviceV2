package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.common.Constants;
import com.community.platform.dto.AdminDashboardPanelVO;
import com.community.platform.dto.DashboardStatsVO;
import com.community.platform.dto.MonthlyMatchRateTrendVO;
import com.community.platform.dto.RegionStatVO;
import com.community.platform.dto.TrendChartVO;
import com.community.platform.dto.NameCountVO;
import com.community.platform.generated.entity.SysUser;
import com.community.platform.generated.mapper.SysUserMapper;
import com.community.platform.security.UserDetailsImpl;
import com.community.platform.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据看板控制器
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 获取统计数据（管理员）
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<DashboardStatsVO> getStats() {
        try {
            SysUser current = getCurrentUser();
            Long scopeCommunityId = isCommunityAdmin(current) ? current.getCommunityId() : null;
            DashboardStatsVO stats = dashboardService.getStats(scopeCommunityId);
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 区域服务覆盖统计（用于全局数据看板热力图）
     */
    @GetMapping("/coverage-by-region")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<List<RegionStatVO>> getRegionCoverage() {
        try {
            SysUser current = getCurrentUser();
            Long scopeCommunityId = isCommunityAdmin(current) ? current.getCommunityId() : null;
            List<RegionStatVO> list = dashboardService.getRegionCoverage(scopeCommunityId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("获取区域覆盖数据失败: " + e.getMessage());
        }
    }

    /**
     * 管理员首页看板聚合（超管含资金/物资占位监控；社区管理员为辖区视角，按绑定 community_id 过滤）
     */
    @GetMapping("/panel")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<AdminDashboardPanelVO> panel() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isSuper = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
            SysUser current = getCurrentUser();
            Long scopeCommunityId = isSuper ? null : current.getCommunityId();
            AdminDashboardPanelVO vo = dashboardService.buildAdminPanel(isSuper, scopeCommunityId);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error("获取看板数据失败: " + e.getMessage());
        }
    }

    /**
     * 供需趋势对比（折线图）
     */
    @GetMapping("/trend")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<TrendChartVO> trend(@RequestParam(defaultValue = "7") int days) {
        SysUser current = getCurrentUser();
        boolean communityAdmin = isCommunityAdmin(current);
        Long scopeCommunityId = communityAdmin ? current.getCommunityId() : null;
        return Result.success(dashboardService.getSupplyDemandTrend(scopeCommunityId, days));
    }

    /**
     * 志愿者荣誉榜 Top10（按近 N 天完成时长）
     */
    @GetMapping("/volunteer-top")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<List<NameCountVO>> volunteerTop(@RequestParam(defaultValue = "30") int days,
                                                  @RequestParam(defaultValue = "10") int topN) {
        SysUser current = getCurrentUser();
        boolean communityAdmin = isCommunityAdmin(current);
        Long scopeCommunityId = communityAdmin ? current.getCommunityId() : null;
        return Result.success(dashboardService.getVolunteerHonorTop(scopeCommunityId, days, topN));
    }

    /**
     * 各社区服务量对比 TopN（按已完成需求数）
     */
    @GetMapping("/community-service-top")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<List<NameCountVO>> communityServiceTop(@RequestParam(defaultValue = "10") int topN) {
        SysUser current = getCurrentUser();
        boolean communityAdmin = isCommunityAdmin(current);
        Long scopeCommunityId = communityAdmin ? current.getCommunityId() : null;
        return Result.success(dashboardService.getCommunityServiceTop(scopeCommunityId, topN));
    }

    /**
     * 月度需求对接成功率趋势（近 N 个月）
     */
    @GetMapping("/monthly-match-rate")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<MonthlyMatchRateTrendVO> monthlyMatchRate(@RequestParam(defaultValue = "6") int months) {
        SysUser current = getCurrentUser();
        boolean communityAdmin = isCommunityAdmin(current);
        Long scopeCommunityId = communityAdmin ? current.getCommunityId() : null;
        return Result.success(dashboardService.getMonthlyMatchRateTrend(scopeCommunityId, months));
    }

    private SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new RuntimeException("未登录");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        SysUser user = sysUserMapper.selectById(userDetails.getUser().getId());
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    private boolean isCommunityAdmin(SysUser user) {
        return user.getRole() != null && user.getRole().equals(Constants.ROLE_COMMUNITY_ADMIN);
    }
}
