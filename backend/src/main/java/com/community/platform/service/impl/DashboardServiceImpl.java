package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.common.Constants;
import com.community.platform.dto.AdminDashboardPanelVO;
import com.community.platform.dto.DashboardStatsVO;
import com.community.platform.dto.FundingMonitorVO;
import com.community.platform.dto.MonthlyMatchRateTrendVO;
import com.community.platform.dto.NameCountVO;
import com.community.platform.dto.RegionStatVO;
import com.community.platform.dto.ScheduleBriefVO;
import com.community.platform.dto.TrendChartVO;
import com.community.platform.generated.entity.ServiceClaim;
import com.community.platform.generated.entity.ServiceRequest;
import com.community.platform.generated.entity.SysRegion;
import com.community.platform.generated.entity.SysUser;
import com.community.platform.generated.mapper.ServiceClaimMapper;
import com.community.platform.generated.mapper.ServiceRequestMapper;
import com.community.platform.generated.mapper.SysRegionMapper;
import com.community.platform.generated.mapper.SysUserMapper;
import com.community.platform.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据看板服务实现
 */
@Service
public class DashboardServiceImpl implements DashboardService {
    
    @Autowired
    private ServiceRequestMapper serviceRequestMapper;
    
    @Autowired
    private ServiceClaimMapper serviceClaimMapper;
    
    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRegionMapper sysRegionMapper;
    
    @Override
    public DashboardStatsVO getStats(Long communityId) {
        DashboardStatsVO stats = new DashboardStatsVO();
        
        try {
            // 平台覆盖规模（用户总数、入驻社区数）
            Long totalUsers = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getIsDeleted, 0));
            stats.setTotalUsers(totalUsers != null ? totalUsers : 0L);
            Long totalCommunities = sysRegionMapper.selectCount(new LambdaQueryWrapper<SysRegion>().eq(SysRegion::getLevel, 3));
            stats.setTotalCommunities(totalCommunities != null ? totalCommunities : 0L);

            // 总需求数
            Long totalRequests = serviceRequestMapper.selectCount(baseRequestWrapper(communityId));
            stats.setTotalRequests(totalRequests != null ? totalRequests : 0L);
        
            // 待审核需求数
            Long pendingRequests = serviceRequestMapper.selectCount(
                    baseRequestWrapper(communityId).eq(ServiceRequest::getStatus, Constants.REQUEST_STATUS_PENDING));
            stats.setPendingRequests(pendingRequests != null ? pendingRequests : 0L);
            
            // 已发布需求数
            Long publishedRequests = serviceRequestMapper.selectCount(
                    baseRequestWrapper(communityId).eq(ServiceRequest::getStatus, Constants.REQUEST_STATUS_PUBLISHED));
            stats.setPublishedRequests(publishedRequests != null ? publishedRequests : 0L);

            // 已认领需求数
            Long claimedRequests = serviceRequestMapper.selectCount(
                    baseRequestWrapper(communityId).eq(ServiceRequest::getStatus, Constants.REQUEST_STATUS_CLAIMED));
            stats.setClaimedRequests(claimedRequests != null ? claimedRequests : 0L);

            // 待确认需求数（志愿者已提交完成，等待需求方确认）
            Long pendingConfirmRequests = serviceRequestMapper.selectCount(
                    baseRequestWrapper(communityId).eq(ServiceRequest::getStatus, Constants.REQUEST_STATUS_PENDING_CONFIRM));
            stats.setPendingConfirmRequests(pendingConfirmRequests != null ? pendingConfirmRequests : 0L);
            
            // 已完成需求数
            Long completedRequests = serviceRequestMapper.selectCount(
                    baseRequestWrapper(communityId).eq(ServiceRequest::getStatus, Constants.REQUEST_STATUS_COMPLETED));
            stats.setCompletedRequests(completedRequests != null ? completedRequests : 0L);

            // 已驳回需求数
            Long rejectedRequests = serviceRequestMapper.selectCount(
                    baseRequestWrapper(communityId).eq(ServiceRequest::getStatus, Constants.REQUEST_STATUS_REJECTED));
            stats.setRejectedRequests(rejectedRequests != null ? rejectedRequests : 0L);
            
            // 总服务时长
            BigDecimal totalServiceHours = serviceClaimMapper.selectList(baseClaimWrapper(communityId)
                    .eq(ServiceClaim::getClaimStatus, Constants.CLAIM_STATUS_COMPLETED)
                    .isNotNull(ServiceClaim::getServiceHours)).stream()
                    .map(ServiceClaim::getServiceHours)
                    .filter(hours -> hours != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stats.setTotalServiceHours(totalServiceHours != null ? totalServiceHours : BigDecimal.ZERO);
            
            // 活跃志愿者数（有完成记录的志愿者）
            // 由于 MyBatis-Plus 的 selectCount 不支持 groupBy，使用 distinct 查询
            Long activeVolunteers = (long) serviceClaimMapper.selectList(baseClaimWrapper(communityId)
                    .eq(ServiceClaim::getClaimStatus, Constants.CLAIM_STATUS_COMPLETED)
                    .select(ServiceClaim::getVolunteerUserId)).stream()
                    .map(ServiceClaim::getVolunteerUserId)
                    .filter(userId -> userId != null)
                    .distinct()
                    .count();
            stats.setActiveVolunteers(activeVolunteers != null ? activeVolunteers : 0L);
            
            // 本月新增需求数
            YearMonth currentMonth = YearMonth.now();
            LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
            LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59, 999000000);
            
            Long monthlyNewRequests = serviceRequestMapper.selectCount(
                    baseRequestWrapper(communityId).between(ServiceRequest::getCreatedAt, monthStart, monthEnd));
            stats.setMonthlyNewRequests(monthlyNewRequests != null ? monthlyNewRequests : 0L);
            
            // 本月完成需求数（需要同时满足：状态为已完成，且完成时间在本月）
            Long monthlyCompletedRequests = serviceRequestMapper.selectCount(
                    baseRequestWrapper(communityId)
                            .eq(ServiceRequest::getStatus, Constants.REQUEST_STATUS_COMPLETED)
                            .isNotNull(ServiceRequest::getCompletedAt)
                            .between(ServiceRequest::getCompletedAt, monthStart, monthEnd));
            stats.setMonthlyCompletedRequests(monthlyCompletedRequests != null ? monthlyCompletedRequests : 0L);

            // 需求对接率 = 已完成需求数 / 总需求数
            if (stats.getTotalRequests() != null && stats.getTotalRequests() > 0) {
                BigDecimal matchRate = BigDecimal.valueOf(stats.getCompletedRequests())
                        .divide(BigDecimal.valueOf(stats.getTotalRequests()), 4, BigDecimal.ROUND_HALF_UP);
                stats.setMatchRate(matchRate);
            } else {
                stats.setMatchRate(BigDecimal.ZERO);
            }

            // 服务覆盖率 = 活跃志愿者数 / 有效志愿者总数（role=3 且 identity=志愿者，且启用）
            Long totalVolunteers = sysUserMapper.selectCount(baseVolunteerWrapper(communityId));
            if (totalVolunteers != null && totalVolunteers > 0) {
                BigDecimal coverageRate = BigDecimal.valueOf(stats.getActiveVolunteers())
                        .divide(BigDecimal.valueOf(totalVolunteers), 4, BigDecimal.ROUND_HALF_UP);
                stats.setCoverageRate(coverageRate);
            } else {
                stats.setCoverageRate(BigDecimal.ZERO);
            }

            // 本周响应时效：发布->认领平均分钟
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.with(DayOfWeek.MONDAY);
            LocalDateTime weekStartAt = weekStart.atStartOfDay();
            List<ServiceRequest> weekClaimed = serviceRequestMapper.selectList(
                    baseRequestWrapper(communityId)
                            .isNotNull(ServiceRequest::getPublishedAt)
                            .isNotNull(ServiceRequest::getClaimedAt)
                            .ge(ServiceRequest::getPublishedAt, weekStartAt)
                            .select(ServiceRequest::getPublishedAt, ServiceRequest::getClaimedAt)
            );
            if (weekClaimed.isEmpty()) {
                stats.setWeeklyAvgResponseMinutes(BigDecimal.ZERO);
            } else {
                long sum = 0;
                long cnt = 0;
                for (ServiceRequest r : weekClaimed) {
                    if (r.getPublishedAt() == null || r.getClaimedAt() == null) continue;
                    long minutes = Duration.between(r.getPublishedAt(), r.getClaimedAt()).toMinutes();
                    if (minutes < 0) continue;
                    sum += minutes;
                    cnt++;
                }
                stats.setWeeklyAvgResponseMinutes(cnt == 0 ? BigDecimal.ZERO
                        : BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(cnt), 1, BigDecimal.ROUND_HALF_UP));
            }

            // 近30天活跃志愿者储备（认领记录）
            LocalDateTime d30 = LocalDateTime.now().minusDays(30);
            long active30 = serviceClaimMapper.selectList(baseClaimWrapper(communityId)
                            .ge(ServiceClaim::getClaimAt, d30)
                            .select(ServiceClaim::getVolunteerUserId))
                    .stream()
                    .map(ServiceClaim::getVolunteerUserId)
                    .filter(x -> x != null)
                    .distinct()
                    .count();
            stats.setActiveVolunteers30d(active30);

            // 系统风险指数（0-100）：用超时未认领/超时未完成规模估算
            LocalDateTime now = LocalDateTime.now();
            Long riskUnclaimed = serviceRequestMapper.selectCount(
                    baseRequestWrapper(communityId)
                            .eq(ServiceRequest::getStatus, Constants.REQUEST_STATUS_PUBLISHED)
                            .lt(ServiceRequest::getExpectedTime, now)
            );
            Long riskIncomplete = serviceRequestMapper.selectCount(
                    baseRequestWrapper(communityId)
                            .in(ServiceRequest::getStatus, Constants.REQUEST_STATUS_CLAIMED, Constants.REQUEST_STATUS_PENDING_CONFIRM)
                            .lt(ServiceRequest::getExpectedTime, now)
            );
            long denom = Math.max(1, stats.getTotalRequests() == null ? 1 : stats.getTotalRequests());
            long r1 = riskUnclaimed == null ? 0 : riskUnclaimed;
            long r2 = riskIncomplete == null ? 0 : riskIncomplete;
            BigDecimal idx = BigDecimal.valueOf((r1 + 2L * r2) * 100.0 / denom).setScale(1, BigDecimal.ROUND_HALF_UP);
            if (idx.compareTo(BigDecimal.valueOf(100)) > 0) idx = BigDecimal.valueOf(100);
            stats.setRiskIndex(idx);
            
            return stats;
        } catch (Exception e) {
            // 记录异常并返回默认值，避免整个接口失败
            e.printStackTrace();
            // 返回部分数据，至少保证接口能正常返回
            if (stats.getTotalRequests() == null) {
                stats.setTotalRequests(0L);
            }
            if (stats.getPendingRequests() == null) {
                stats.setPendingRequests(0L);
            }
            if (stats.getPublishedRequests() == null) {
                stats.setPublishedRequests(0L);
            }
            if (stats.getCompletedRequests() == null) {
                stats.setCompletedRequests(0L);
            }
            if (stats.getTotalServiceHours() == null) {
                stats.setTotalServiceHours(BigDecimal.ZERO);
            }
            if (stats.getActiveVolunteers() == null) {
                stats.setActiveVolunteers(0L);
            }
            if (stats.getMonthlyNewRequests() == null) {
                stats.setMonthlyNewRequests(0L);
            }
            if (stats.getMonthlyCompletedRequests() == null) {
                stats.setMonthlyCompletedRequests(0L);
            }
            throw new RuntimeException("获取统计数据时发生错误: " + e.getMessage(), e);
        }
    }

    @Override
    public List<RegionStatVO> getRegionCoverage(Long communityId) {
        // 全城公益热力图：按 sys_region.province 聚合；若无 province 则回退 CN
        List<ServiceRequest> completed = serviceRequestMapper.selectList(
                baseRequestWrapper(communityId)
                        .eq(ServiceRequest::getStatus, Constants.REQUEST_STATUS_COMPLETED)
                        .select(ServiceRequest::getCommunityId)
        );
        if (completed.isEmpty()) {
            RegionStatVO vo = new RegionStatVO();
            vo.setRegionCode("CN");
            vo.setServiceCount(0L);
            return List.of(vo);
        }
        List<Long> communityIds = completed.stream()
                .map(ServiceRequest::getCommunityId)
                .filter(x -> x != null)
                .distinct()
                .toList();
        Map<Long, SysRegion> regionMap = communityIds.isEmpty()
                ? Map.of()
                : sysRegionMapper.selectBatchIds(communityIds).stream()
                .collect(Collectors.toMap(SysRegion::getId, r -> r, (a, b) -> a));

        Map<String, Long> grouped = completed.stream()
                .map(r -> {
                    SysRegion region = r.getCommunityId() == null ? null : regionMap.get(r.getCommunityId());
                    if (region != null && region.getProvince() != null && !region.getProvince().isBlank()) {
                        return region.getProvince();
                    }
                    return "CN";
                })
                .collect(Collectors.groupingBy(x -> x, Collectors.counting()));

        return grouped.entrySet().stream().map(e -> {
            RegionStatVO vo = new RegionStatVO();
            vo.setRegionCode(e.getKey());
            vo.setServiceCount(e.getValue());
            return vo;
        }).toList();
    }

    @Override
    public List<NameCountVO> getDemandByServiceType(Long communityId) {
        List<ServiceRequest> list = serviceRequestMapper.selectList(
                baseRequestWrapper(communityId)
                        .select(ServiceRequest::getServiceType));
        Map<String, Long> grouped = list.stream()
                .map(ServiceRequest::getServiceType)
                .map(t -> t == null || t.isBlank() ? "其他" : t)
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));
        return grouped.entrySet().stream()
                .map(e -> {
                    NameCountVO vo = new NameCountVO();
                    vo.setName(e.getKey());
                    vo.setCount(e.getValue());
                    return vo;
                })
                .sorted(Comparator.comparingLong(NameCountVO::getCount).reversed())
                .toList();
    }

    @Override
    public FundingMonitorVO getFundingMonitorPlaceholder() {
        FundingMonitorVO vo = new FundingMonitorVO();
        vo.setFundIn(BigDecimal.ZERO);
        vo.setFundOut(BigDecimal.ZERO);
        vo.setMaterialIn(BigDecimal.ZERO);
        vo.setMaterialOut(BigDecimal.ZERO);
        vo.setNote("当前展示为平台统计口径预留项：未接入独立财务/物资子系统前默认记为 0。");
        return vo;
    }

    @Override
    public List<ScheduleBriefVO> getUpcomingSchedule(int limit, Long communityId) {
        List<ServiceRequest> rows = serviceRequestMapper.selectList(
                baseRequestWrapper(communityId)
                        .isNotNull(ServiceRequest::getExpectedTime)
                        .ge(ServiceRequest::getExpectedTime, LocalDateTime.now())
                        .in(ServiceRequest::getStatus, Arrays.asList(
                                Constants.REQUEST_STATUS_PENDING,
                                Constants.REQUEST_STATUS_PUBLISHED,
                                Constants.REQUEST_STATUS_CLAIMED,
                                Constants.REQUEST_STATUS_PENDING_CONFIRM))
                        .orderByAsc(ServiceRequest::getExpectedTime)
                        .last("LIMIT " + Math.max(1, Math.min(limit, 50))));
        List<ScheduleBriefVO> out = new ArrayList<>();
        for (ServiceRequest r : rows) {
            ScheduleBriefVO vo = new ScheduleBriefVO();
            vo.setId(r.getId());
            vo.setServiceType(r.getServiceType());
            vo.setExpectedTime(r.getExpectedTime());
            vo.setServiceAddress(r.getServiceAddress());
            vo.setStatus(r.getStatus() != null ? r.getStatus().intValue() : null);
            out.add(vo);
        }
        return out;
    }

    @Override
    public AdminDashboardPanelVO buildAdminPanel(boolean superAdmin, Long communityId) {
        AdminDashboardPanelVO vo = new AdminDashboardPanelVO();
        vo.setScope(superAdmin ? "SUPER_ADMIN" : "COMMUNITY_ADMIN");
        vo.setStats(getStats(communityId));
        vo.setRegionCoverage(getRegionCoverage(communityId));
        vo.setDemandByServiceType(getDemandByServiceType(communityId));
        vo.setUpcomingSchedule(getUpcomingSchedule(12, communityId));
        if (superAdmin) {
            vo.setFundingMonitor(getFundingMonitorPlaceholder());
        }
        return vo;
    }

    @Override
    public TrendChartVO getSupplyDemandTrend(Long communityId, int days) {
        int d = Math.max(3, Math.min(days, 30));
        LocalDate today = LocalDate.now();
        List<String> labels = new ArrayList<>();
        List<Long> demand = new ArrayList<>();
        List<Long> supply = new ArrayList<>();
        for (int i = d - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(23, 59, 59, 999000000);
            labels.add(String.format("%02d-%02d", date.getMonthValue(), date.getDayOfMonth()));
            Long demandCount = serviceRequestMapper.selectCount(
                    baseRequestWrapper(communityId).between(ServiceRequest::getCreatedAt, start, end));
            demand.add(demandCount == null ? 0L : demandCount);
            Long supplyCount = serviceClaimMapper.selectCount(
                    baseClaimWrapper(communityId).between(ServiceClaim::getClaimAt, start, end));
            supply.add(supplyCount == null ? 0L : supplyCount);
        }
        TrendChartVO vo = new TrendChartVO();
        vo.setLabels(labels);
        vo.setDemand(demand);
        vo.setSupply(supply);
        return vo;
    }

    @Override
    public List<NameCountVO> getVolunteerHonorTop(Long communityId, int days, int topN) {
        int d = Math.max(1, Math.min(days, 365));
        int n = Math.max(3, Math.min(topN, 50));
        LocalDateTime start = LocalDateTime.now().minusDays(d);
        List<ServiceClaim> claims = serviceClaimMapper.selectList(
                baseClaimWrapper(communityId)
                        .ge(ServiceClaim::getClaimAt, start)
                        .eq(ServiceClaim::getClaimStatus, Constants.CLAIM_STATUS_COMPLETED)
                        .isNotNull(ServiceClaim::getServiceHours)
                        .select(ServiceClaim::getVolunteerUserId, ServiceClaim::getServiceHours)
        );
        if (claims.isEmpty()) {
            return List.of();
        }
        Map<Long, BigDecimal> sumHours = claims.stream()
                .filter(c -> c.getVolunteerUserId() != null && c.getServiceHours() != null)
                .collect(Collectors.groupingBy(ServiceClaim::getVolunteerUserId,
                        Collectors.reducing(BigDecimal.ZERO, ServiceClaim::getServiceHours, BigDecimal::add)));
        List<Long> ids = sumHours.keySet().stream().toList();
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        return sumHours.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(n)
                .map(e -> {
                    SysUser u = userMap.get(e.getKey());
                    NameCountVO vo = new NameCountVO();
                    vo.setName(u != null && u.getRealName() != null ? u.getRealName() : String.valueOf(e.getKey()));
                    vo.setCount(e.getValue().setScale(0, BigDecimal.ROUND_HALF_UP).longValue());
                    return vo;
                })
                .toList();
    }

    @Override
    public List<NameCountVO> getCommunityServiceTop(Long communityId, int topN) {
        int n = Math.max(3, Math.min(topN, 50));
        List<ServiceRequest> requests = serviceRequestMapper.selectList(
                baseRequestWrapper(communityId)
                        .in(ServiceRequest::getStatus,
                                Constants.REQUEST_STATUS_PUBLISHED,
                                Constants.REQUEST_STATUS_CLAIMED,
                                Constants.REQUEST_STATUS_PENDING_CONFIRM,
                                Constants.REQUEST_STATUS_COMPLETED)
                        .isNotNull(ServiceRequest::getCommunityId)
                        .select(ServiceRequest::getCommunityId)
        );
        if (requests.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> grouped = requests.stream()
                .map(ServiceRequest::getCommunityId)
                .filter(id -> id != null)
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        List<Long> regionIds = grouped.keySet().stream().toList();
        Map<Long, SysRegion> regionMap = sysRegionMapper.selectBatchIds(regionIds).stream()
                .collect(Collectors.toMap(SysRegion::getId, r -> r, (a, b) -> a));

        return grouped.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(n)
                .map(e -> {
                    SysRegion region = regionMap.get(e.getKey());
                    String regionName = region != null && region.getName() != null && !region.getName().isBlank()
                            ? region.getName()
                            : ("社区#" + e.getKey());
                    NameCountVO vo = new NameCountVO();
                    vo.setName(regionName);
                    vo.setCount(e.getValue());
                    return vo;
                })
                .toList();
    }

    @Override
    public MonthlyMatchRateTrendVO getMonthlyMatchRateTrend(Long communityId, int months) {
        int m = Math.max(3, Math.min(months, 24));
        YearMonth end = YearMonth.now();
        YearMonth startYm = end.minusMonths(m - 1L);

        List<String> labels = new ArrayList<>();
        List<BigDecimal> rates = new ArrayList<>();
        List<Long> created = new ArrayList<>();
        List<Long> completed = new ArrayList<>();

        YearMonth cursor = startYm;
        while (!cursor.isAfter(end)) {
            LocalDateTime monthStart = cursor.atDay(1).atStartOfDay();
            LocalDateTime monthEnd = cursor.atEndOfMonth().atTime(23, 59, 59, 999000000);

            Long createdCnt = serviceRequestMapper.selectCount(
                    baseRequestWrapper(communityId).between(ServiceRequest::getCreatedAt, monthStart, monthEnd));
            Long completedCnt = serviceRequestMapper.selectCount(
                    baseRequestWrapper(communityId)
                            .eq(ServiceRequest::getStatus, Constants.REQUEST_STATUS_COMPLETED)
                            .isNotNull(ServiceRequest::getCompletedAt)
                            .between(ServiceRequest::getCompletedAt, monthStart, monthEnd));

            long denom = createdCnt == null ? 0L : createdCnt;
            long num = completedCnt == null ? 0L : completedCnt;

            BigDecimal pct = BigDecimal.ZERO;
            if (denom > 0) {
                pct = BigDecimal.valueOf(num)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(denom), 1, BigDecimal.ROUND_HALF_UP);
            }

            labels.add(String.format("%04d-%02d", cursor.getYear(), cursor.getMonthValue()));
            rates.add(pct);
            created.add(denom);
            completed.add(num);

            cursor = cursor.plusMonths(1);
        }

        MonthlyMatchRateTrendVO vo = new MonthlyMatchRateTrendVO();
        vo.setLabels(labels);
        vo.setSuccessRatePercent(rates);
        vo.setCreatedCount(created);
        vo.setCompletedCount(completed);
        return vo;
    }

    private LambdaQueryWrapper<ServiceRequest> baseRequestWrapper(Long communityId) {
        LambdaQueryWrapper<ServiceRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceRequest::getIsDeleted, 0);
        if (communityId != null) {
            wrapper.eq(ServiceRequest::getCommunityId, communityId);
        }
        return wrapper;
    }

    private LambdaQueryWrapper<ServiceClaim> baseClaimWrapper(Long communityId) {
        LambdaQueryWrapper<ServiceClaim> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceClaim::getIsDeleted, 0);
        if (communityId != null) {
            wrapper.inSql(ServiceClaim::getRequestId,
                    "SELECT id FROM service_request WHERE is_deleted=0 AND community_id=" + communityId);
        }
        return wrapper;
    }

    private LambdaQueryWrapper<SysUser> baseVolunteerWrapper(Long communityId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getIsDeleted, 0)
                .eq(SysUser::getRole, Constants.ROLE_NORMAL_USER)
                .eq(SysUser::getIdentityType, Constants.IDENTITY_VOLUNTEER)
                .eq(SysUser::getStatus, 1);
        if (communityId != null) {
            wrapper.eq(SysUser::getCommunityId, communityId);
        }
        return wrapper;
    }
}
