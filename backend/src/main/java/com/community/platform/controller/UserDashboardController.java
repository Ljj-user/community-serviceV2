package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.dto.UserDashboardSummaryVO;
import com.community.platform.security.UserDetailsImpl;
import com.community.platform.service.UserDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 普通用户（居民/志愿者）首页看板
 */
@RestController
@RequestMapping("/user/dashboard")
public class UserDashboardController {

    @Autowired
    private UserDashboardService userDashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('USER')")
    public Result<UserDashboardSummaryVO> summary() {
        try {
            Long userId = getCurrentUserId();
            return Result.success(userDashboardService.buildSummary(userId));
        } catch (Exception e) {
            return Result.error("获取个人看板失败: " + e.getMessage());
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getUser().getId();
        }
        throw new RuntimeException("未登录");
    }
}
