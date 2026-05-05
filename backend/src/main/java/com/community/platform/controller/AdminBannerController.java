package com.community.platform.controller;

import com.community.platform.common.Constants;
import com.community.platform.common.Result;
import com.community.platform.dto.AdminBannerUpsertRequest;
import com.community.platform.dto.BannerVO;
import com.community.platform.security.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Admin banner compatibility API. The menu is hidden, but keeping the endpoint
 * prevents old routes from failing while the product scope is being reduced.
 */
@RestController
@RequestMapping("/admin/banner")
public class AdminBannerController {
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Value("${app.banner-base-url:/static/banner/}")
    private String bannerBaseUrl;
    @Value("${app.banner-upload-dir:uploads/banner}")
    private String bannerUploadDir;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<List<BannerVO>> list(@RequestParam(value = "communityId", required = false) Long communityId) {
        try {
            Operator op = currentOperator();
            Long scope = resolveScope(op, communityId);
            return Result.success(query(scope));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<Void> upsert(@Valid @RequestBody AdminBannerUpsertRequest req) {
        try {
            Operator op = currentOperator();
            Long scope = resolveScope(op, req.getCommunityId());
            if (req.getId() == null) {
                jdbcTemplate.update(
                        """
                        INSERT INTO community_banner(community_id,title,image_url,link_url,sort_no,status,created_at,updated_at)
                        VALUES(?,?,?,?,?,?,NOW(3),NOW(3))
                        """,
                        scope,
                        req.getTitle().trim(),
                        trimOrNull(req.getImageUrl()),
                        trimOrNull(req.getLinkUrl()),
                        req.getSortNo(),
                        req.getStatus()
                );
            } else {
                Integer cnt = jdbcTemplate.queryForObject(
                        "SELECT COUNT(1) FROM community_banner WHERE id=? AND ((? IS NULL AND community_id IS NULL) OR community_id=?)",
                        Integer.class,
                        req.getId(),
                        scope,
                        scope
                );
                if (cnt == null || cnt <= 0) {
                    return Result.error("记录不存在或无权限");
                }
                jdbcTemplate.update(
                        """
                        UPDATE community_banner
                        SET title=?, image_url=?, link_url=?, sort_no=?, status=?, updated_at=NOW(3)
                        WHERE id=?
                        """,
                        req.getTitle().trim(),
                        trimOrNull(req.getImageUrl()),
                        trimOrNull(req.getLinkUrl()),
                        req.getSortNo(),
                        req.getStatus(),
                        req.getId()
                );
            }
            return Result.success("保存成功", null);
        } catch (Exception e) {
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<Void> delete(@PathVariable("id") Long id,
                               @RequestParam(value = "communityId", required = false) Long communityId) {
        try {
            Operator op = currentOperator();
            Long scope = resolveScope(op, communityId);
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM community_banner WHERE id=? AND ((? IS NULL AND community_id IS NULL) OR community_id=?)",
                    Integer.class,
                    id,
                    scope,
                    scope
            );
            if (cnt == null || cnt <= 0) {
                return Result.error("记录不存在或无权限");
            }
            jdbcTemplate.update("DELETE FROM community_banner WHERE id=?", id);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload-image")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件为空");
        }
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String extLower = ext == null ? "" : ext.toLowerCase(Locale.ROOT);
        String contentType = file.getContentType();
        boolean extAllowed = !extLower.isEmpty() && ALLOWED_EXT.contains(extLower);
        boolean mimeLooksImage = contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/");
        if (!mimeLooksImage && !extAllowed) {
            return Result.error("仅支持图片类型文件");
        }
        String filename = UUID.randomUUID().toString().replace("-", "");
        if (extAllowed) filename = filename + "." + extLower;

        File uploadRoot = new File(bannerUploadDir).getAbsoluteFile();
        if (!uploadRoot.exists() && !uploadRoot.mkdirs()) {
            return Result.error("上传失败: 无法创建目录");
        }
        File dest = new File(uploadRoot, filename);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            return Result.error("上传失败: " + e.getMessage());
        }

        String base = bannerBaseUrl == null ? "/static/banner/" : bannerBaseUrl.trim();
        if (!base.endsWith("/")) base = base + "/";
        return Result.success(Map.of("imageUrl", base + filename));
    }

    private List<BannerVO> query(Long scopeCommunityId) {
        return jdbcTemplate.query(
                """
                SELECT b.id,b.community_id,r.name AS community_name,b.title,b.image_url,b.link_url
                FROM community_banner b
                LEFT JOIN sys_region r ON r.id=b.community_id
                WHERE ((? IS NULL AND b.community_id IS NULL) OR b.community_id=?)
                ORDER BY sort_no ASC, id ASC
                LIMIT 50
                """,
                (rs, rowNum) -> {
                    BannerVO vo = new BannerVO();
                    vo.setId(rs.getLong("id"));
                    Long cid = rs.getObject("community_id") == null ? null : rs.getLong("community_id");
                    vo.setCommunityId(cid);
                    vo.setCommunityName(rs.getString("community_name"));
                    vo.setTitle(rs.getString("title"));
                    vo.setSubtitle("社区公益服务");
                    vo.setImageUrl(rs.getString("image_url"));
                    vo.setLinkUrl(rs.getString("link_url"));
                    return vo;
                },
                scopeCommunityId,
                scopeCommunityId
        );
    }

    private static String trimOrNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static Long resolveScope(Operator op, Long requested) {
        if (Constants.ROLE_COMMUNITY_ADMIN.equals(op.role)) {
            if (op.communityId == null) throw new RuntimeException("当前社区管理员未绑定社区");
            if (requested != null && !requested.equals(op.communityId)) {
                throw new RuntimeException("社区管理员只能管理本社区轮播图");
            }
            return op.communityId;
        }
        return requested;
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
    }
}
