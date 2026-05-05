package com.community.platform.controller;

import com.community.platform.common.Constants;
import com.community.platform.common.Result;
import com.community.platform.dto.AdminInviteCodeCreateRequest;
import com.community.platform.dto.AdminInviteCodeVO;
import com.community.platform.generated.entity.SysRegion;
import com.community.platform.generated.mapper.SysRegionMapper;
import com.community.platform.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * 管理端：社区邀请码生成/禁用/列表
 */
@RestController
@RequestMapping("/admin/invite-code")
public class AdminInviteCodeController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private SysRegionMapper sysRegionMapper;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<List<AdminInviteCodeVO>> list(@RequestParam(value = "communityId", required = false) Long communityId) {
        try {
            Operator op = currentOperator();
            Long scopeCommunityId = resolveListScope(op, communityId);
            List<AdminInviteCodeVO> list = jdbcTemplate.query(
                    "SELECT c.id,c.community_id,r.name AS community_name,c.code,c.status,c.expires_at,c.max_uses,c.used_count,c.created_at " +
                            "FROM community_invite_code c " +
                            "LEFT JOIN sys_region r ON r.id=c.community_id " +
                            "WHERE (? IS NULL OR c.community_id=?) " +
                            "ORDER BY c.id DESC LIMIT 200",
                    (rs, rowNum) -> {
                        AdminInviteCodeVO vo = new AdminInviteCodeVO();
                        vo.setId(rs.getLong("id"));
                        vo.setCommunityId(rs.getLong("community_id"));
                        vo.setCommunityName(rs.getString("community_name"));
                        vo.setCode(rs.getString("code"));
                        vo.setStatus(rs.getInt("status"));
                        Timestamp exp = rs.getTimestamp("expires_at");
                        vo.setExpiresAt(exp == null ? null : exp.toLocalDateTime());
                        vo.setMaxUses(rs.getInt("max_uses"));
                        vo.setUsedCount(rs.getInt("used_count"));
                        Timestamp ct = rs.getTimestamp("created_at");
                        vo.setCreatedAt(ct == null ? null : ct.toLocalDateTime());
                        return vo;
                    },
                    scopeCommunityId, scopeCommunityId
            );
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<AdminInviteCodeVO> create(@Valid @RequestBody AdminInviteCodeCreateRequest request) {
        try {
            Operator op = currentOperator();
            Long communityId = resolveCommunityId(op, request.getCommunityId());
            SysRegion region = sysRegionMapper.selectById(communityId);
            if (region == null) {
                return Result.error("社区不存在（communityId 必须对应 sys_region.id）");
            }
            LocalDateTime expiresAt = null;
            if (request.getExpiresInDays() != null && request.getExpiresInDays() > 0) {
                expiresAt = LocalDateTime.now().plusDays(request.getExpiresInDays());
            }
            int maxUses = request.getMaxUses() == null ? 100 : request.getMaxUses();
            if (maxUses <= 0) {
                return Result.error("maxUses 必须大于 0");
            }

            String code = insertWithRetry(communityId, expiresAt, maxUses, op.userId);

            AdminInviteCodeVO vo = new AdminInviteCodeVO();
            vo.setCommunityId(communityId);
            vo.setCommunityName(region.getName());
            vo.setCode(code);
            vo.setStatus(1);
            vo.setExpiresAt(expiresAt);
            vo.setMaxUses(maxUses);
            vo.setUsedCount(0);
            vo.setCreatedAt(LocalDateTime.now());
            return Result.success("创建成功", vo);
        } catch (Exception e) {
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<Void> disable(@PathVariable("id") Long id) {
        try {
            Operator op = currentOperator();
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM community_invite_code WHERE id=? AND (? IS NULL OR community_id=?)",
                    Integer.class, id, op.scopeCommunityId, op.scopeCommunityId
            );
            if (cnt == null || cnt <= 0) {
                return Result.error("记录不存在或无权限");
            }
            jdbcTemplate.update("UPDATE community_invite_code SET status=0, updated_at=NOW(3) WHERE id=?", id);
            return Result.success("已禁用", null);
        } catch (Exception e) {
            return Result.error("禁用失败: " + e.getMessage());
        }
    }

    private String insertWithRetry(Long communityId, LocalDateTime expiresAt, int maxUses, Long createdBy) {
        for (int i = 0; i < 10; i++) {
            String code = randomCode(8);
            try {
                jdbcTemplate.update(
                        "INSERT INTO community_invite_code(community_id,code,status,expires_at,max_uses,used_count,created_by,created_at,updated_at) " +
                                "VALUES(?,?,1,?,?,0,?,NOW(3),NOW(3))",
                        communityId, code, expiresAt, maxUses, createdBy
                );
                return code;
            } catch (DuplicateKeyException ignored) {
                // retry
            }
        }
        throw new RuntimeException("生成邀请码失败，请重试");
    }

    private static String randomCode(int len) {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 去掉易混淆字符
        Random r = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private static Long resolveCommunityId(Operator op, Long requestCommunityId) {
        if (op.role != null && op.role == Constants.ROLE_COMMUNITY_ADMIN) {
            if (op.scopeCommunityId == null) {
                throw new RuntimeException("当前社区管理员未绑定社区，无法生成邀请码");
            }
            if (requestCommunityId != null && !requestCommunityId.equals(op.scopeCommunityId)) {
                throw new RuntimeException("社区管理员只能为本社区生成邀请码");
            }
            return op.scopeCommunityId;
        }
        // 超管：必须指定
        if (requestCommunityId == null) {
            throw new RuntimeException("communityId 不能为空（系统管理员生成邀请码需指定社区）");
        }
        return requestCommunityId;
    }

    private static Long resolveListScope(Operator op, Long requestCommunityId) {
        if (op.role != null && op.role == Constants.ROLE_COMMUNITY_ADMIN) {
            if (op.scopeCommunityId == null) {
                throw new RuntimeException("当前社区管理员未绑定社区，无法查看邀请码");
            }
            if (requestCommunityId != null && !requestCommunityId.equals(op.scopeCommunityId)) {
                throw new RuntimeException("社区管理员只能查看本社区邀请码");
            }
            return op.scopeCommunityId;
        }
        return requestCommunityId;
    }

    private Operator currentOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new RuntimeException("未登录");
        }
        Operator op = new Operator();
        op.userId = userDetails.getUser().getId();
        op.role = userDetails.getUser().getRole();
        if (op.role != null && op.role == Constants.ROLE_COMMUNITY_ADMIN) {
            op.scopeCommunityId = userDetails.getUser().getCommunityId();
        } else {
            op.scopeCommunityId = null;
        }
        return op;
    }

    private static class Operator {
        Long userId;
        Byte role;
        Long scopeCommunityId;
    }
}

