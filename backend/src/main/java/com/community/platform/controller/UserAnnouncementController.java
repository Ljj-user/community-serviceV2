package com.community.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.community.platform.common.Result;
import com.community.platform.dto.AnnouncementVO;
import com.community.platform.dto.UserAnnouncementQueryDTO;
import com.community.platform.service.UserAnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 普通用户公告（只读）
 */
@RestController
@RequestMapping("/user/announcements")
public class UserAnnouncementController {

    @Autowired
    private UserAnnouncementService userAnnouncementService;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('USER', 'COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<IPage<AnnouncementVO>> list(UserAnnouncementQueryDTO queryDTO) {
        try {
            return Result.success(userAnnouncementService.list(queryDTO));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<AnnouncementVO> detail(@PathVariable("id") Long id) {
        try {
            return Result.success(userAnnouncementService.detail(id));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}

