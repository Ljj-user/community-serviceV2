package com.community.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.community.platform.common.Result;
import com.community.platform.dto.MobileAlertCardVO;
import com.community.platform.dto.NotificationUnreadCountVO;
import com.community.platform.dto.UserNotificationVO;
import com.community.platform.security.UserDetailsImpl;
import com.community.platform.service.UserNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 当前用户站内消息
 */
@RestController
@RequestMapping("/notifications")
public class UserNotificationController {

    @Autowired
    private UserNotificationService userNotificationService;

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl)) {
            throw new RuntimeException("未登录");
        }
        return ((UserDetailsImpl) auth.getPrincipal()).getUser().getId();
    }

    @GetMapping("/unread-count")
    public Result<NotificationUnreadCountVO> unreadCount() {
        return Result.success(userNotificationService.countUnread(currentUserId()));
    }

    @GetMapping("/mine")
    public Result<IPage<UserNotificationVO>> mine(
            @RequestParam(value = "category", required = false) Byte category,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "20") long size) {
        return Result.success(userNotificationService.pageMine(currentUserId(), category, page, size));
    }

    /**
     * 移动端预警消息入口（仅返回异常检测相关站内信）
     */
    @GetMapping("/mobile/alerts")
    public Result<IPage<MobileAlertCardVO>> mobileAlerts(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "20") long size,
            @RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly) {
        return Result.success(userNotificationService.pageMyAlertNotifications(currentUserId(), page, size, unreadOnly));
    }

    /**
     * 移动端预警未读数量
     */
    @GetMapping("/mobile/alerts/unread-count")
    public Result<Long> mobileAlertUnreadCount() {
        return Result.success(userNotificationService.countMyAlertUnread(currentUserId()));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable("id") Long id) {
        userNotificationService.markRead(currentUserId(), id);
        return Result.success("已标记已读", null);
    }

    @PutMapping("/mark-all-read")
    public Result<Void> markAllRead(@RequestParam(value = "category", required = false) Byte category) {
        userNotificationService.markAllRead(currentUserId(), category);
        return Result.success("已全部标记已读", null);
    }
}
