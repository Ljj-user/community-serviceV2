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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端：异常预警处理闭环
 */
@RestController
@RequestMapping("/admin/alerts")
public class AdminAlertController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<PageResult<Map<String, Object>>> list(@RequestParam(value = "status", required = false) Integer status,
                                                        @RequestParam(value = "page", defaultValue = "1") long page,
                                                        @RequestParam(value = "size", defaultValue = "20") long size) {
        Operator op = currentUser();
        Long scope = op.scopeCommunityId();
        long offset = Math.max(0, page - 1) * size;
        Integer total = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM anomaly_alert_event a
                WHERE (? IS NULL OR a.status=?)
                  AND (? IS NULL OR a.community_id=?)
                """, Integer.class, status, status, scope, scope);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT a.id,
                       a.alert_code AS ruleCode,
                       a.alert_code AS alertType,
                       a.severity AS alertLevel,
                       a.community_id AS communityId,
                       a.target_user_id AS targetUserId,
                       a.alert_code AS title,
                       CONCAT(a.trigger_rule, IF(a.suggestion_action IS NULL OR a.suggestion_action='', '', CONCAT('；建议动作：', a.suggestion_action))) AS description,
                       a.status,
                       a.handler_user_id AS handlerUserId,
                       a.handle_result AS handleResult,
                       a.occurred_at AS occurredAt,
                       a.handled_at AS handledAt,
                       r.name AS communityName,
                       u.real_name AS targetUserName,
                       h.real_name AS handlerName
                FROM anomaly_alert_event a
                LEFT JOIN sys_region r ON r.id=a.community_id
                LEFT JOIN sys_user u ON u.id=a.target_user_id
                LEFT JOIN sys_user h ON h.id=a.handler_user_id
                WHERE (? IS NULL OR a.status=?)
                  AND (? IS NULL OR a.community_id=?)
                ORDER BY a.occurred_at DESC
                LIMIT ? OFFSET ?
                """, status, status, scope, scope, size, offset);
        return Result.success(PageResult.of(rows, total == null ? 0 : total, page, size));
    }

    @PostMapping("/{id}/handle")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<Void> handle(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> body) {
        Operator op = currentUser();
        String result = body == null || body.get("handleResult") == null ? "已跟进处理" : String.valueOf(body.get("handleResult"));
        int updated = jdbcTemplate.update("""
                UPDATE anomaly_alert_event
                SET status=2, handler_user_id=?, handled_at=NOW(3), handle_result=?
                WHERE id=? AND (? IS NULL OR community_id=?)
                """, op.userId, result, id, op.scopeCommunityId(), op.scopeCommunityId());
        if (updated <= 0) {
            throw new RuntimeException("预警不存在或无权限处理");
        }
        return Result.success("处理完成", null);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, Object>> detail(@PathVariable("id") Long id) {
        Operator op = currentUser();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT a.id,
                       a.alert_code AS ruleCode,
                       a.alert_code AS alertType,
                       a.severity AS alertLevel,
                       a.community_id AS communityId,
                       a.target_user_id AS targetUserId,
                       a.alert_code AS title,
                       CONCAT(a.trigger_rule, IF(a.suggestion_action IS NULL OR a.suggestion_action='', '', CONCAT('；建议动作：', a.suggestion_action))) AS description,
                       a.status,
                       a.handler_user_id AS handlerUserId,
                       a.handle_result AS handleResult,
                       a.occurred_at AS occurredAt,
                       a.handled_at AS handledAt,
                       r.name AS communityName,
                       u.real_name AS targetUserName,
                       h.real_name AS handlerName
                FROM anomaly_alert_event a
                LEFT JOIN sys_region r ON r.id=a.community_id
                LEFT JOIN sys_user u ON u.id=a.target_user_id
                LEFT JOIN sys_user h ON h.id=a.handler_user_id
                WHERE a.id=?
                  AND (? IS NULL OR a.community_id=?)
                LIMIT 1
                """, id, op.scopeCommunityId(), op.scopeCommunityId());
        if (rows.isEmpty()) {
            return Result.error("预警记录不存在");
        }
        return Result.success(rows.get(0));
    }

    private Operator currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new RuntimeException("用户未登录");
        }
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
