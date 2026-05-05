package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.common.Constants;
import com.community.platform.dto.AnnouncementBriefVO;
import com.community.platform.dto.ResidentDashboardVO;
import com.community.platform.dto.UserDashboardSummaryVO;
import com.community.platform.dto.VolunteerDashboardVO;
import com.community.platform.generated.entity.Announcement;
import com.community.platform.generated.entity.ServiceClaim;
import com.community.platform.generated.entity.ServiceEvaluation;
import com.community.platform.generated.entity.ServiceRequest;
import com.community.platform.generated.entity.SysUser;
import com.community.platform.generated.mapper.AnnouncementMapper;
import com.community.platform.generated.mapper.ServiceClaimMapper;
import com.community.platform.generated.mapper.ServiceEvaluationMapper;
import com.community.platform.generated.mapper.ServiceRequestMapper;
import com.community.platform.generated.mapper.SysUserMapper;
import com.community.platform.service.UserDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class UserDashboardServiceImpl implements UserDashboardService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private ServiceRequestMapper serviceRequestMapper;

    @Autowired
    private ServiceClaimMapper serviceClaimMapper;

    @Autowired
    private ServiceEvaluationMapper serviceEvaluationMapper;

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public UserDashboardSummaryVO buildSummary(Long userId) {
        SysUser u = sysUserMapper.selectById(userId);
        if (u == null || (u.getIsDeleted() != null && u.getIsDeleted() != 0)) {
            throw new RuntimeException("用户不存在");
        }
        if (!Constants.ROLE_NORMAL_USER.equals(u.getRole())) {
            throw new RuntimeException("仅普通用户可使用该接口");
        }
        UserDashboardSummaryVO vo = new UserDashboardSummaryVO();
        if (isCertifiedVolunteer(userId)) {
            vo.setPanelType("VOLUNTEER");
            vo.setVolunteer(buildVolunteer(userId));
        } else {
            vo.setPanelType("RESIDENT");
            vo.setResident(buildResident(userId));
        }
        return vo;
    }

    private boolean isCertifiedVolunteer(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM volunteer_profile WHERE user_id=? AND cert_status=2",
                Integer.class, userId
        );
        return count != null && count > 0;
    }

    private ResidentDashboardVO buildResident(Long userId) {
        ResidentDashboardVO vo = new ResidentDashboardVO();
        List<ServiceRequest> mine = serviceRequestMapper.selectList(
                new LambdaQueryWrapper<ServiceRequest>()
                        .eq(ServiceRequest::getIsDeleted, 0)
                        .eq(ServiceRequest::getRequesterUserId, userId));
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("pending", 0L);
        counts.put("published", 0L);
        counts.put("claimed", 0L);
        counts.put("completed", 0L);
        counts.put("rejected", 0L);
        for (ServiceRequest r : mine) {
            Byte st = r.getStatus();
            if (st == null) continue;
            int v = st.intValue();
            switch (v) {
                case 0 -> counts.merge("pending", 1L, Long::sum);
                case 1 -> counts.merge("published", 1L, Long::sum);
                case 2 -> counts.merge("claimed", 1L, Long::sum);
                case 3 -> counts.merge("completed", 1L, Long::sum);
                case 4 -> counts.merge("rejected", 1L, Long::sum);
                default -> { }
            }
        }
        vo.setRequestStatusCounts(counts);

        Long evalCount = serviceEvaluationMapper.selectCount(
                new LambdaQueryWrapper<ServiceEvaluation>()
                        .eq(ServiceEvaluation::getIsDeleted, 0)
                        .eq(ServiceEvaluation::getResidentUserId, userId));
        vo.setEvaluationsGivenCount(evalCount != null ? evalCount : 0L);

        List<Announcement> ann = announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getIsDeleted, 0)
                        .eq(Announcement::getStatus, 1)
                        .eq(Announcement::getTargetScope, 0)
                        .orderByDesc(Announcement::getPublishedAt)
                        .last("LIMIT 5"));
        List<AnnouncementBriefVO> brief = new ArrayList<>();
        for (Announcement a : ann) {
            AnnouncementBriefVO b = new AnnouncementBriefVO();
            b.setId(a.getId());
            b.setTitle(a.getTitle());
            b.setPublishedAt(a.getPublishedAt());
            brief.add(b);
        }
        vo.setLatestAnnouncements(brief);
        return vo;
    }

    private VolunteerDashboardVO buildVolunteer(Long userId) {
        VolunteerDashboardVO vo = new VolunteerDashboardVO();
        List<ServiceClaim> claims = serviceClaimMapper.selectList(
                new LambdaQueryWrapper<ServiceClaim>()
                        .eq(ServiceClaim::getIsDeleted, 0)
                        .eq(ServiceClaim::getVolunteerUserId, userId));

        long pending = claims.stream().filter(c -> Objects.equals(c.getClaimStatus(), Constants.CLAIM_STATUS_CLAIMED)).count();
        long done = claims.stream().filter(c -> Objects.equals(c.getClaimStatus(), Constants.CLAIM_STATUS_COMPLETED)).count();
        vo.setPendingClaimCount(pending);
        vo.setCompletedClaimCount(done);

        BigDecimal hours = claims.stream()
                .filter(c -> Objects.equals(c.getClaimStatus(), Constants.CLAIM_STATUS_COMPLETED))
                .map(ServiceClaim::getServiceHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalServiceHours(hours);

        List<ServiceEvaluation> evals = serviceEvaluationMapper.selectList(
                new LambdaQueryWrapper<ServiceEvaluation>()
                        .eq(ServiceEvaluation::getIsDeleted, 0)
                        .eq(ServiceEvaluation::getVolunteerUserId, userId));
        vo.setEvaluationCount((long) evals.size());
        if (!evals.isEmpty()) {
            double avg = evals.stream()
                    .mapToInt(e -> e.getRating() != null ? e.getRating().intValue() : 0)
                    .average()
                    .orElse(0);
            vo.setAverageRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        }

        List<ServiceClaim> recentDone = claims.stream()
                .filter(c -> Objects.equals(c.getClaimStatus(), Constants.CLAIM_STATUS_COMPLETED))
                .sorted((a, b) -> {
                    if (a.getHoursSubmittedAt() == null) return 1;
                    if (b.getHoursSubmittedAt() == null) return -1;
                    return b.getHoursSubmittedAt().compareTo(a.getHoursSubmittedAt());
                })
                .limit(5)
                .toList();
        List<String> titles = new ArrayList<>();
        for (ServiceClaim c : recentDone) {
            ServiceRequest req = serviceRequestMapper.selectById(c.getRequestId());
            if (req != null && req.getServiceType() != null) {
                titles.add(req.getServiceType());
            }
        }
        vo.setRecentProjectTitles(titles);
        List<BigDecimal> snapshot = jdbcTemplate.query("""
                SELECT credit_score, avg_rating_30d, completion_rate_30d
                FROM volunteer_credit_snapshot
                WHERE user_id=?
                """, (rs, rowNum) -> List.of(
                rs.getBigDecimal("credit_score"),
                rs.getBigDecimal("avg_rating_30d"),
                rs.getBigDecimal("completion_rate_30d")
        ), userId).stream().findFirst().orElse(null);
        if (snapshot != null) {
            vo.setCreditScore(snapshot.get(0));
            vo.setAvgRating30d(snapshot.get(1));
            vo.setCompletionRate30d(snapshot.get(2));
        } else {
            vo.setCreditScore(BigDecimal.ZERO);
            vo.setAvgRating30d(null);
            vo.setCompletionRate30d(BigDecimal.ZERO);
        }
        vo.setHonorNote("荣誉值由累计服务时长、近30天评价与履约率综合计算，按统一信用规则动态更新。");
        return vo;
    }
}
