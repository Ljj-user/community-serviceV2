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
 * 管理端：社区加入申请审核
 */
@RestController
@RequestMapping("/admin/community-join")
public class AdminCommunityJoinController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<PageResult<Map<String, Object>>> list(@RequestParam(value = "status", required = false) Integer status,
                                                        @RequestParam(value = "page", defaultValue = "1") long page,
                                                        @RequestParam(value = "size", defaultValue = "20") long size) {
        Operator op = currentOperator();
        long offset = Math.max(0, page - 1) * size;
        Long scope = op.role.equals(Constants.ROLE_COMMUNITY_ADMIN) ? op.communityId : null;
        Integer total = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM community_join_application a
                WHERE (? IS NULL OR a.status=?)
                  AND (? IS NULL OR a.community_id=?)
                """, Integer.class, status, status, scope, scope);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT a.id,
                       a.user_id AS userId,
                       a.community_id AS communityId,
                       a.invite_code AS inviteCode,
                       a.real_name AS realName,
                       a.phone,
                       a.address,
                       CONCAT_WS('，', a.real_name, a.phone, a.address) AS applyReason,
                       a.status,
                       a.reject_reason AS rejectReason,
                       a.reviewer_user_id AS reviewerUserId,
                       a.created_at AS createdAt,
                       a.updated_at AS updatedAt,
                       a.reviewed_at AS reviewedAt,
                       u.username,
                       r.name AS communityName,
                       reviewer.real_name AS reviewerName
                FROM community_join_application a
                LEFT JOIN sys_user u ON u.id=a.user_id
                LEFT JOIN sys_region r ON r.id=a.community_id
                LEFT JOIN sys_user reviewer ON reviewer.id=a.reviewer_user_id
                WHERE (? IS NULL OR a.status=?)
                  AND (? IS NULL OR a.community_id=?)
                ORDER BY a.created_at DESC
                LIMIT ? OFFSET ?
                """, status, status, scope, scope, size, offset);
        return Result.success(PageResult.of(rows, total == null ? 0 : total, page, size));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    @Transactional
    public Result<Void> approve(@PathVariable("id") Long id) {
        Operator op = currentOperator();
        Map<String, Object> app = loadApplicationForReview(id, op);
        Long userId = ((Number) app.get("user_id")).longValue();
        Long communityId = ((Number) app.get("community_id")).longValue();
        String inviteCode = app.get("invite_code") == null ? null : String.valueOf(app.get("invite_code"));

        jdbcTemplate.update("""
                UPDATE community_join_application
                SET status=1, reviewer_user_id=?, reviewed_at=NOW(3), reject_reason=NULL, updated_at=NOW(3)
                WHERE id=? AND status=0
                """, op.userId, id);
        jdbcTemplate.update("""
                UPDATE sys_user
                SET community_id=?, community_join_status=2, updated_at=NOW(3)
                WHERE id=? AND is_deleted=0
                """, communityId, userId);
        if (inviteCode != null && !inviteCode.isBlank()) {
            jdbcTemplate.update("""
                    UPDATE community_invite_code
                    SET used_count=used_count+1, updated_at=NOW(3)
                    WHERE code=? AND status=1 AND used_count < max_uses
                    """, inviteCode);
        }
        return Result.success("审核通过", null);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    @Transactional
    public Result<Void> reject(@PathVariable("id") Long id,
                               @RequestBody(required = false) Map<String, Object> body) {
        Operator op = currentOperator();
        Map<String, Object> app = loadApplicationForReview(id, op);
        Long userId = ((Number) app.get("user_id")).longValue();
        String reason = body == null || body.get("rejectReason") == null
                ? "申请信息不符合社区审核要求"
                : String.valueOf(body.get("rejectReason"));
        jdbcTemplate.update("""
                UPDATE community_join_application
                SET status=2, reviewer_user_id=?, reviewed_at=NOW(3), reject_reason=?, updated_at=NOW(3)
                WHERE id=? AND status=0
                """, op.userId, reason, id);
        jdbcTemplate.update("""
                UPDATE sys_user
                SET community_join_status=3, updated_at=NOW(3)
                WHERE id=? AND is_deleted=0
                """, userId);
        return Result.success("已拒绝", null);
    }

    private Map<String, Object> loadApplicationForReview(Long id, Operator op) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT * FROM community_join_application
                WHERE id=? AND status=0
                  AND (? IS NULL OR community_id=?)
                LIMIT 1
                """, id, op.scopeCommunityId(), op.scopeCommunityId());
        if (rows.isEmpty()) {
            throw new RuntimeException("申请不存在、已处理或无权限");
        }
        return rows.get(0);
    }

    private Operator currentOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new RuntimeException("未登录");
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
