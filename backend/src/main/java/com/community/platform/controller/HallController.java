package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.dto.HallSummaryVO;
import com.community.platform.security.UserDetailsImpl;
import com.community.platform.service.HallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 大厅（移动端）业务接口
 */
@RestController
@RequestMapping("/hall")
public class HallController {

    @Autowired
    private HallService hallService;

    /**
     * 大厅-服务概览汇总
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('USER')")
    public Result<HallSummaryVO> summary() {
        try {
            Long userId = getCurrentUserId();
            return Result.success(hallService.getSummary(userId));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
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

