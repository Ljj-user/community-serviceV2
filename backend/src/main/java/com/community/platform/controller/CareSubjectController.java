package com.community.platform.controller;

import com.community.platform.common.Constants;
import com.community.platform.common.Result;
import com.community.platform.dto.PageResult;
import com.community.platform.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端：重点关怀对象管理
 */
@RestController
@RequestMapping("/admin/care-subject")
public class CareSubjectController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<PageResult<Map<String, Object>>> list(@RequestParam(value = "careType", required = false) String careType,
                                                        @RequestParam(value = "page", defaultValue = "1") long page,
                                                        @RequestParam(value = "size", defaultValue = "20") long size) {
        Operator op = currentUser();
        Long scope = op.scopeCommunityId();
        long offset = Math.max(0, page - 1) * size;
        Integer total = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM care_subject_profile c
                WHERE c.is_deleted=0
                  AND (? IS NULL OR c.care_type=?)
                  AND (? IS NULL OR c.community_id=?)
                """, Integer.class, careType, careType, scope, scope);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT c.id,
                       c.user_id AS userId,
                       c.community_id AS communityId,
                       c.care_type AS careType,
                       c.care_level AS careLevel,
                       c.living_status AS livingStatus,
                       c.health_note AS healthNote,
                       c.emergency_contact_name AS emergencyContactName,
                       c.emergency_contact_phone AS emergencyContactPhone,
                       c.emergency_contact_relation AS emergencyContactRelation,
                       c.monitor_enabled AS monitorEnabled,
                       c.last_visit_at AS lastVisitAt,
                       c.created_by AS createdBy,
                       c.created_at AS createdAt,
                       c.updated_at AS updatedAt,
                       u.username,
                       u.real_name AS realName,
                       u.phone,
                       u.address,
                       r.name AS communityName
                FROM care_subject_profile c
                LEFT JOIN sys_user u ON u.id=c.user_id
                LEFT JOIN sys_region r ON r.id=c.community_id
                WHERE c.is_deleted=0
                  AND (? IS NULL OR c.care_type=?)
                  AND (? IS NULL OR c.community_id=?)
                ORDER BY c.care_level DESC, c.updated_at DESC
                LIMIT ? OFFSET ?
                """, careType, careType, scope, scope, size, offset);
        return Result.success(PageResult.of(rows, total == null ? 0 : total, page, size));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    @Transactional
    public Result<Void> save(@RequestBody Map<String, Object> body) {
        Operator op = currentUser();
        Long userId = longValue(body.get("userId"));
        Long communityId = Constants.ROLE_COMMUNITY_ADMIN.equals(op.role) ? op.communityId : longValue(body.get("communityId"));
        if (userId == null || communityId == null) throw new RuntimeException("userId 和 communityId 不能为空");
        if (Constants.ROLE_COMMUNITY_ADMIN.equals(op.role) && !communityId.equals(op.communityId)) {
            throw new RuntimeException("只能管理本社区重点对象");
        }
        jdbcTemplate.update("""
                INSERT INTO care_subject_profile(user_id,community_id,care_type,care_level,living_status,health_note,
                  emergency_contact_name,emergency_contact_phone,emergency_contact_relation,monitor_enabled,last_visit_at,created_by,created_at,updated_at,is_deleted)
                VALUES(?,?,?,?,?,?,?,?,?,?,NULL,?,NOW(3),NOW(3),0)
                ON DUPLICATE KEY UPDATE
                  community_id=VALUES(community_id),
                  care_type=VALUES(care_type),
                  care_level=VALUES(care_level),
                  living_status=VALUES(living_status),
                  health_note=VALUES(health_note),
                  emergency_contact_name=VALUES(emergency_contact_name),
                  emergency_contact_phone=VALUES(emergency_contact_phone),
                  emergency_contact_relation=VALUES(emergency_contact_relation),
                  monitor_enabled=VALUES(monitor_enabled),
                  updated_at=NOW(3),
                  is_deleted=0
                """,
                userId,
                communityId,
                text(body.get("careType"), "独居老人"),
                intValue(body.get("careLevel"), 2),
                text(body.get("livingStatus"), null),
                text(body.get("healthNote"), null),
                text(body.get("emergencyContactName"), null),
                text(body.get("emergencyContactPhone"), null),
                text(body.get("emergencyContactRelation"), null),
                intValue(body.get("monitorEnabled"), 1),
                op.userId
        );
        jdbcTemplate.update("UPDATE sys_user SET identity_tag=?, updated_at=NOW(3) WHERE id=?", text(body.get("careType"), "重点关怀对象"), userId);
        return Result.success("保存成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        Operator op = currentUser();
        int updated = jdbcTemplate.update("""
                UPDATE care_subject_profile SET is_deleted=1, updated_at=NOW(3)
                WHERE id=? AND (? IS NULL OR community_id=?)
                """, id, op.scopeCommunityId(), op.scopeCommunityId());
        if (updated <= 0) throw new RuntimeException("记录不存在或无权限");
        return Result.success("删除成功", null);
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

    private static Long longValue(Object raw) {
        if (raw == null || String.valueOf(raw).isBlank()) return null;
        return Long.valueOf(String.valueOf(raw));
    }

    private static int intValue(Object raw, int def) {
        if (raw == null || String.valueOf(raw).isBlank()) return def;
        return Integer.parseInt(String.valueOf(raw));
    }

    private static String text(Object raw, String def) {
        if (raw == null || String.valueOf(raw).isBlank()) return def;
        return String.valueOf(raw).trim();
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
