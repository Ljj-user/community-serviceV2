package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.dto.BannerVO;
import com.community.platform.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Mobile home banners.
 */
@RestController
@RequestMapping("/user/banner")
public class UserBannerController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/list")
    @PreAuthorize("hasRole('USER')")
    public Result<List<BannerVO>> list() {
        try {
            Long userId = getCurrentUserId();
            Long communityId = jdbcTemplate.queryForObject(
                    "SELECT community_id FROM sys_user WHERE id=? LIMIT 1",
                    Long.class,
                    userId
            );

            List<BannerVO> scoped = queryBanners(communityId);
            if (!scoped.isEmpty()) {
                return Result.success(scoped);
            }
            List<BannerVO> global = queryBanners(null);
            return Result.success(global.isEmpty() ? fallbackBanners() : global);
        } catch (Exception e) {
            return Result.success(fallbackBanners());
        }
    }

    private List<BannerVO> queryBanners(Long communityId) {
        return jdbcTemplate.query(
                """
                SELECT id, title, image_url, link_url
                FROM community_banner
                WHERE status=1 AND ((? IS NULL AND community_id IS NULL) OR community_id=?)
                ORDER BY sort_no ASC, id ASC
                LIMIT 10
                """,
                (rs, rowNum) -> {
                    BannerVO vo = new BannerVO();
                    vo.setId(rs.getLong("id"));
                    vo.setTitle(rs.getString("title"));
                    vo.setSubtitle("社区公益服务");
                    vo.setImageUrl(rs.getString("image_url"));
                    vo.setLinkUrl(rs.getString("link_url"));
                    return vo;
                },
                communityId,
                communityId
        );
    }

    private List<BannerVO> fallbackBanners() {
        return List.of(
                banner(-1L, "社区公益服务", "邻里互助，让帮助更近", "https://picsum.photos/seed/community-service-1/960/420"),
                banner(-2L, "重点关怀行动", "独居老人、残障居民优先响应", "https://picsum.photos/seed/community-care-2/960/420"),
                banner(-3L, "志愿者认证开放", "有技能，就能参与公益服务", "https://picsum.photos/seed/community-volunteer-3/960/420")
        );
    }

    private BannerVO banner(Long id, String title, String subtitle, String imageUrl) {
        BannerVO vo = new BannerVO();
        vo.setId(id);
        vo.setTitle(title);
        vo.setSubtitle(subtitle);
        vo.setImageUrl(imageUrl);
        return vo;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getUser().getId();
        }
        throw new RuntimeException("未登录");
    }
}
