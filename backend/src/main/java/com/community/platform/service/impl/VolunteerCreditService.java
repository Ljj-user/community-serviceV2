package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.generated.entity.ServiceClaim;
import com.community.platform.generated.entity.ServiceEvaluation;
import com.community.platform.generated.entity.ServiceRequest;
import com.community.platform.generated.mapper.ServiceClaimMapper;
import com.community.platform.generated.mapper.ServiceEvaluationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 志愿者信用计算服务
 */
@Service
public class VolunteerCreditService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ServiceEvaluationMapper serviceEvaluationMapper;

    @Autowired
    private ServiceClaimMapper serviceClaimMapper;

    public void onServiceConfirmed(ServiceRequest request, ServiceClaim claim) {
        if (request == null || claim == null || claim.getVolunteerUserId() == null || claim.getId() == null) {
            return;
        }
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM volunteer_credit_ledger WHERE claim_id=?",
                Integer.class, claim.getId()
        );
        if (exists != null && exists > 0) {
            refreshSnapshot(claim.getVolunteerUserId());
            return;
        }
        BigDecimal hours = claim.getServiceHours() == null ? BigDecimal.ZERO : claim.getServiceHours();
        ServiceEvaluation evaluation = serviceEvaluationMapper.selectOne(
                new LambdaQueryWrapper<ServiceEvaluation>()
                        .eq(ServiceEvaluation::getClaimId, claim.getId())
                        .eq(ServiceEvaluation::getIsDeleted, 0)
                        .orderByDesc(ServiceEvaluation::getCreatedAt)
                        .last("LIMIT 1")
        );
        Integer rating = evaluation == null || evaluation.getRating() == null ? null : evaluation.getRating().intValue();
        BigDecimal ratingFactor = ratingFactor(rating);
        BigDecimal overtimePenalty = overtimePenalty(request);
        BigDecimal delta = hours.multiply(BigDecimal.TEN)
                .multiply(ratingFactor)
                .multiply(overtimePenalty)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal capped = delta.min(new BigDecimal("500.00"));

        jdbcTemplate.update("""
                INSERT INTO volunteer_credit_ledger(user_id, request_id, claim_id, hours, rating, overtime_penalty,
                credit_delta, calc_version, created_at)
                VALUES(?,?,?,?,?,?,?,?,NOW())
                """, claim.getVolunteerUserId(), request.getId(), claim.getId(), hours, rating, overtimePenalty, capped, "v1");
        refreshSnapshot(claim.getVolunteerUserId());
    }

    private void refreshSnapshot(Long userId) {
        BigDecimal totalHours = serviceClaimMapper.selectList(
                new LambdaQueryWrapper<ServiceClaim>()
                        .eq(ServiceClaim::getIsDeleted, 0)
                        .eq(ServiceClaim::getVolunteerUserId, userId)
                        .eq(ServiceClaim::getClaimStatus, (byte) 2)
                        .isNotNull(ServiceClaim::getServiceHours)
                        .select(ServiceClaim::getServiceHours)
        ).stream().map(ServiceClaim::getServiceHours).reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime d30 = LocalDateTime.now().minusDays(30);
        Double avgRating = jdbcTemplate.queryForObject("""
                SELECT AVG(rating) FROM service_evaluation
                WHERE is_deleted=0 AND volunteer_user_id=? AND created_at>=?
                """, Double.class, userId, d30);
        Double completionRate = jdbcTemplate.queryForObject("""
                SELECT
                  CASE WHEN total_cnt=0 THEN 0 ELSE completed_cnt/total_cnt END
                FROM (
                  SELECT
                    SUM(CASE WHEN claim_status=2 THEN 1 ELSE 0 END) AS completed_cnt,
                    COUNT(1) AS total_cnt
                  FROM service_claim
                  WHERE is_deleted=0 AND volunteer_user_id=? AND claim_at>=?
                ) t
                """, Double.class, userId, d30);
        BigDecimal creditScore = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(credit_delta), 0) FROM volunteer_credit_ledger WHERE user_id=?
                """, BigDecimal.class, userId);
        if (creditScore == null) creditScore = BigDecimal.ZERO;
        jdbcTemplate.update("""
                INSERT INTO volunteer_credit_snapshot(user_id, total_hours, avg_rating_30d, completion_rate_30d, credit_score, updated_at)
                VALUES(?,?,?,?,?,NOW())
                ON DUPLICATE KEY UPDATE
                  total_hours=VALUES(total_hours),
                  avg_rating_30d=VALUES(avg_rating_30d),
                  completion_rate_30d=VALUES(completion_rate_30d),
                  credit_score=VALUES(credit_score),
                  updated_at=NOW()
                """,
                userId,
                totalHours == null ? BigDecimal.ZERO : totalHours.setScale(2, RoundingMode.HALF_UP),
                avgRating == null ? null : BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP),
                completionRate == null ? BigDecimal.ZERO : BigDecimal.valueOf(completionRate).setScale(2, RoundingMode.HALF_UP),
                creditScore.setScale(2, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal ratingFactor(Integer rating) {
        if (rating == null) return BigDecimal.ONE;
        return switch (rating) {
            case 5 -> new BigDecimal("1.10");
            case 4 -> new BigDecimal("1.05");
            case 3 -> BigDecimal.ONE;
            case 2 -> new BigDecimal("0.95");
            case 1 -> new BigDecimal("0.90");
            default -> BigDecimal.ONE;
        };
    }

    private BigDecimal overtimePenalty(ServiceRequest request) {
        if (request.getExpectedTime() == null || request.getCompletedAt() == null) {
            return BigDecimal.ONE;
        }
        long overtimeHours = java.time.Duration.between(request.getExpectedTime(), request.getCompletedAt()).toHours();
        if (overtimeHours <= 0) return BigDecimal.ONE;
        if (overtimeHours <= 24) return new BigDecimal("0.95");
        return new BigDecimal("0.90");
    }
}
