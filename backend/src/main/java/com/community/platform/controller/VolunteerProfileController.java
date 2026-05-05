package com.community.platform.controller;

import com.community.platform.common.Constants;
import com.community.platform.common.Result;
import com.community.platform.dto.PageResult;
import com.community.platform.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 志愿者认证与能力信息
 */
@RestController
public class VolunteerProfileController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/volunteer/profile")
    @PreAuthorize("hasRole('USER')")
    public Result<Map<String, Object>> myProfile() {
        Long userId = currentUser().userId;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id,
                       user_id AS userId,
                       community_id AS communityId,
                       cert_status AS certStatus,
                       real_name AS realName,
                       id_card_no AS idCardNo,
                       skill_tags AS skillTags,
                       service_radius_km AS serviceRadiusKm,
                       available_time AS availableTime,
                       reviewer_user_id AS reviewerUserId,
                       certified_at AS certifiedAt,
                       reject_reason AS rejectReason,
                       created_at AS createdAt,
                       updated_at AS updatedAt
                FROM volunteer_profile
                WHERE user_id=? LIMIT 1
                """, userId);
        return Result.success(rows.isEmpty() ? null : rows.get(0));
    }

    @PostMapping("/volunteer/apply")
    @PreAuthorize("hasRole('USER')")
    @Transactional
    public Result<Void> apply(@RequestBody Map<String, Object> body) {
        Operator op = currentUser();
        Long communityId = op.communityId;
        if (communityId == null) {
            throw new RuntimeException("请先加入社区后再申请志愿者认证");
        }
        String skillJson = toJson(body.get("skillTags"));
        String availableTime = body.get("availableTime") == null ? null : String.valueOf(body.get("availableTime"));
        BigDecimal radius = toDecimal(body.get("serviceRadiusKm"));
        String idCardNo = body.get("idCardNo") == null ? null : String.valueOf(body.get("idCardNo"));
        jdbcTemplate.update("""
                INSERT INTO volunteer_profile(user_id,community_id,cert_status,real_name,id_card_no,skill_tags,service_radius_km,available_time,created_at,updated_at)
                SELECT id, community_id, 1, real_name, ?, CAST(? AS JSON), ?, ?, NOW(3), NOW(3)
                FROM sys_user WHERE id=? AND is_deleted=0
                ON DUPLICATE KEY UPDATE
                  community_id=VALUES(community_id),
                  cert_status=1,
                  id_card_no=VALUES(id_card_no),
                  skill_tags=VALUES(skill_tags),
                  service_radius_km=VALUES(service_radius_km),
                  available_time=VALUES(available_time),
                  reject_reason=NULL,
                  updated_at=NOW(3)
                """, idCardNo, skillJson, radius, availableTime, op.userId);
        jdbcTemplate.update("DELETE FROM sys_user_skill WHERE user_id=?", op.userId);
        for (String tag : parseTags(body.get("skillTags"))) {
            jdbcTemplate.update("INSERT IGNORE INTO sys_user_skill(user_id,skill_tag,created_at) VALUES(?,?,NOW(3))", op.userId, tag);
        }
        jdbcTemplate.update("UPDATE sys_user SET skill_tags=CAST(? AS JSON), updated_at=NOW(3) WHERE id=?", skillJson, op.userId);
        return Result.success("志愿者认证申请已提交", null);
    }

    @GetMapping("/admin/volunteer/list")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<PageResult<Map<String, Object>>> list(@RequestParam(value = "certStatus", required = false) Integer certStatus,
                                                        @RequestParam(value = "page", defaultValue = "1") long page,
                                                        @RequestParam(value = "size", defaultValue = "20") long size) {
        Operator op = currentUser();
        Long scope = op.scopeCommunityId();
        long offset = Math.max(0, page - 1) * size;
        Integer total = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM volunteer_profile v
                WHERE (? IS NULL OR v.cert_status=?)
                  AND (? IS NULL OR v.community_id=?)
                """, Integer.class, certStatus, certStatus, scope, scope);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT v.id,
                       v.user_id AS userId,
                       v.community_id AS communityId,
                       v.cert_status AS certStatus,
                       v.real_name AS realName,
                       v.id_card_no AS idCardNo,
                       v.skill_tags AS skillTags,
                       v.service_radius_km AS serviceRadiusKm,
                       v.available_time AS availableTime,
                       v.reviewer_user_id AS reviewerUserId,
                       v.certified_at AS certifiedAt,
                       v.reject_reason AS rejectReason,
                       v.created_at AS createdAt,
                       v.updated_at AS updatedAt,
                       u.username,
                       u.phone,
                       r.name AS communityName,
                       reviewer.real_name AS reviewerName
                FROM volunteer_profile v
                LEFT JOIN sys_user u ON u.id=v.user_id
                LEFT JOIN sys_region r ON r.id=v.community_id
                LEFT JOIN sys_user reviewer ON reviewer.id=v.reviewer_user_id
                WHERE (? IS NULL OR v.cert_status=?)
                  AND (? IS NULL OR v.community_id=?)
                ORDER BY v.updated_at DESC
                LIMIT ? OFFSET ?
                """, certStatus, certStatus, scope, scope, size, offset);
        return Result.success(PageResult.of(rows, total == null ? 0 : total, page, size));
    }

    @PostMapping("/admin/volunteer/{id}/approve")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    @Transactional
    public Result<Void> approve(@PathVariable("id") Long id) {
        Operator op = currentUser();
        Map<String, Object> profile = loadProfile(id, op);
        Long userId = ((Number) profile.get("user_id")).longValue();
        jdbcTemplate.update("""
                UPDATE volunteer_profile
                SET cert_status=2, reviewer_user_id=?, certified_at=NOW(3), reject_reason=NULL, updated_at=NOW(3)
                WHERE id=?
                """, op.userId, id);
        jdbcTemplate.update("UPDATE sys_user SET identity_type=?, updated_at=NOW(3) WHERE id=?", Constants.IDENTITY_VOLUNTEER, userId);
        return Result.success("认证通过", null);
    }

    @PostMapping("/admin/volunteer/{id}/reject")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<Void> reject(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> body) {
        Operator op = currentUser();
        loadProfile(id, op);
        String reason = body == null || body.get("rejectReason") == null ? "认证资料未通过审核" : String.valueOf(body.get("rejectReason"));
        jdbcTemplate.update("""
                UPDATE volunteer_profile
                SET cert_status=3, reviewer_user_id=?, reject_reason=?, updated_at=NOW(3)
                WHERE id=?
                """, op.userId, reason, id);
        return Result.success("已拒绝", null);
    }

    private Map<String, Object> loadProfile(Long id, Operator op) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM volunteer_profile WHERE id=? AND (? IS NULL OR community_id=?) LIMIT 1
                """, id, op.scopeCommunityId(), op.scopeCommunityId());
        if (rows.isEmpty()) throw new RuntimeException("志愿者资料不存在或无权限");
        return rows.get(0);
    }

    private String toJson(Object raw) {
        try {
            return objectMapper.writeValueAsString(raw == null ? List.of() : raw);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> parseTags(Object raw) {
        if (raw instanceof List<?> list) {
            return list.stream().filter(x -> x != null && !String.valueOf(x).isBlank()).map(x -> String.valueOf(x).trim()).toList();
        }
        return List.of();
    }

    private BigDecimal toDecimal(Object raw) {
        if (raw == null || String.valueOf(raw).isBlank()) return null;
        return new BigDecimal(String.valueOf(raw));
    }

    private Operator currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl userDetails)) throw new RuntimeException("未登录");
        Operator op = new Operator();
        op.userId = userDetails.getUser().getId();
        op.role = userDetails.getUser().getRole();
        op.communityId = userDetails.getUser().getCommunityId();
        return op;
    }

    private static class Operator {
        Long userId;
        Byte role;
        Long communityId;
        Long scopeCommunityId() {
            return Constants.ROLE_COMMUNITY_ADMIN.equals(role) ? communityId : null;
        }
    }
}
