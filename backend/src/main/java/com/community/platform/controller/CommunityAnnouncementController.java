package com.community.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.community.platform.common.Result;
import com.community.platform.dto.AnnouncementQueryDTO;
import com.community.platform.dto.AnnouncementSaveDTO;
import com.community.platform.dto.AnnouncementVO;
import com.community.platform.security.UserDetailsImpl;
import com.community.platform.service.CommunityAnnouncementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 信息发布与公告（社区管理员）
 */
@RestController
@RequestMapping("/community/announcements")
public class CommunityAnnouncementController {

    @Autowired
    private CommunityAnnouncementService communityAnnouncementService;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<IPage<AnnouncementVO>> list(AnnouncementQueryDTO queryDTO) {
        try {
            Long userId = getCurrentUserId();
            return Result.success(communityAnnouncementService.list(userId, queryDTO));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<AnnouncementVO> detail(@PathVariable("id") Long id) {
        try {
            Long userId = getCurrentUserId();
            return Result.success(communityAnnouncementService.detail(userId, id));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<Long> create(@Valid @RequestBody AnnouncementSaveDTO dto) {
        try {
            Long userId = getCurrentUserId();
            dto.setId(null);
            Long id = communityAnnouncementService.save(userId, dto);
            return Result.success("发布成功", id);
        } catch (Exception e) {
            return Result.error("发布失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<Long> update(@PathVariable("id") Long id, @Valid @RequestBody AnnouncementSaveDTO dto) {
        try {
            Long userId = getCurrentUserId();
            dto.setId(id);
            Long savedId = communityAnnouncementService.save(userId, dto);
            return Result.success("保存成功", savedId);
        } catch (Exception e) {
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/top")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<Void> setTop(@PathVariable("id") Long id, @RequestParam("isTop") Boolean isTop) {
        try {
            Long userId = getCurrentUserId();
            communityAnnouncementService.setTop(id, Boolean.TRUE.equals(isTop), userId);
            return Result.success(Boolean.TRUE.equals(isTop) ? "已置顶" : "已取消置顶", null);
        } catch (Exception e) {
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        try {
            Long userId = getCurrentUserId();
            communityAnnouncementService.delete(id, userId);
            return Result.success("已删除", null);
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getUser().getId();
        }
        throw new RuntimeException("未登录");
    }
}

