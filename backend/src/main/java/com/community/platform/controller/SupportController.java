package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.service.SupportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支持与点赞相关接口
 */
@RestController
@RequestMapping("/support")
public class SupportController {

    @Autowired
    private SupportService supportService;

    /**
     * 获取全局点赞总数
     */
    @GetMapping("/like-count")
    @PreAuthorize("hasAnyRole('USER','COMMUNITY_ADMIN','SUPER_ADMIN')")
    public Result<Long> getLikeCount() {
        return Result.success(supportService.getLikeCount());
    }

    /**
     * 点赞一次，返回最新总数
     */
    @PostMapping("/like")
    @PreAuthorize("hasAnyRole('USER','COMMUNITY_ADMIN','SUPER_ADMIN')")
    public Result<Long> like() {
        return Result.success(supportService.addLike());
    }
}

