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
 * 便民信息查询与管理
 */
@RestController
public class ConvenienceInfoController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/convenience-info/list")
    @PreAuthorize("hasAnyRole('USER', 'COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<List<Map<String, Object>>> userList(@RequestParam(value = "category", required = false) String category) {
        Operator op = currentUser();
        if (op.communityId == null && Constants.ROLE_NORMAL_USER.equals(op.role)) {
            return Result.success(List.of());
        }
        Long scope = Constants.ROLE_SUPER_ADMIN.equals(op.role) ? null : op.communityId;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id,
                       community_id AS communityId,
                       category,
                       title,
                       content,
                       contact_phone AS contactPhone,
                       address,
                       sort_no AS sortNo,
                       status,
                       created_by AS createdBy,
                       created_at AS createdAt,
                       updated_at AS updatedAt
                FROM convenience_info
                WHERE is_deleted=0 AND status=1
                  AND (? IS NULL OR community_id=?)
                  AND (? IS NULL OR category=?)
                ORDER BY sort_no ASC, id DESC
                """, scope, scope, category, category);
        return Result.success(rows);
    }

    @GetMapping("/admin/convenience-info/list")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<PageResult<Map<String, Object>>> adminList(@RequestParam(value = "category", required = false) String category,
                                                            @RequestParam(value = "page", defaultValue = "1") long page,
                                                            @RequestParam(value = "size", defaultValue = "20") long size) {
        Operator op = currentUser();
        Long scope = op.scopeCommunityId();
        long offset = Math.max(0, page - 1) * size;
        Integer total = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM convenience_info
                WHERE is_deleted=0
                  AND (? IS NULL OR community_id=?)
                  AND (? IS NULL OR category=?)
                """, Integer.class, scope, scope, category, category);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT c.id,
                       c.community_id AS communityId,
                       c.category,
                       c.title,
                       c.content,
                       c.contact_phone AS contactPhone,
                       c.address,
                       c.sort_no AS sortNo,
                       c.status,
                       c.created_by AS createdBy,
                       c.created_at AS createdAt,
                       c.updated_at AS updatedAt,
                       r.name AS communityName
                FROM convenience_info c
                LEFT JOIN sys_region r ON r.id=c.community_id
                WHERE c.is_deleted=0
                  AND (? IS NULL OR c.community_id=?)
                  AND (? IS NULL OR c.category=?)
                ORDER BY c.sort_no ASC, c.id DESC
                LIMIT ? OFFSET ?
                """, scope, scope, category, category, size, offset);
        return Result.success(PageResult.of(rows, total == null ? 0 : total, page, size));
    }

    @PostMapping("/admin/convenience-info")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<Void> save(@RequestBody Map<String, Object> body) {
        Operator op = currentUser();
        Long id = longValue(body.get("id"));
        Long communityId = Constants.ROLE_COMMUNITY_ADMIN.equals(op.role) ? op.communityId : longValue(body.get("communityId"));
        if (communityId == null) throw new RuntimeException("communityId 不能为空");
        if (id == null) {
            jdbcTemplate.update("""
                    INSERT INTO convenience_info(community_id,category,title,content,contact_phone,address,sort_no,status,created_by,created_at,updated_at,is_deleted)
                    VALUES(?,?,?,?,?,?,?,?,?,NOW(3),NOW(3),0)
                    """, communityId, text(body.get("category"), "社区电话"), text(body.get("title"), "便民信息"),
                    text(body.get("content"), null), text(body.get("contactPhone"), null), text(body.get("address"), null),
                    intValue(body.get("sortNo"), 0), intValue(body.get("status"), 1), op.userId);
        } else {
            jdbcTemplate.update("""
                    UPDATE convenience_info
                    SET category=?, title=?, content=?, contact_phone=?, address=?, sort_no=?, status=?, updated_at=NOW(3)
                    WHERE id=? AND (? IS NULL OR community_id=?)
                    """, text(body.get("category"), "社区电话"), text(body.get("title"), "便民信息"),
                    text(body.get("content"), null), text(body.get("contactPhone"), null), text(body.get("address"), null),
                    intValue(body.get("sortNo"), 0), intValue(body.get("status"), 1), id, op.scopeCommunityId(), op.scopeCommunityId());
        }
        return Result.success("保存成功", null);
    }

    @DeleteMapping("/admin/convenience-info/{id}")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        Operator op = currentUser();
        int updated = jdbcTemplate.update("""
                UPDATE convenience_info SET is_deleted=1, updated_at=NOW(3)
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
