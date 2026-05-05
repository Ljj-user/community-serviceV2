package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.dto.ServiceEvaluationDTO;
import com.community.platform.dto.ServiceEvaluationHistoryVO;
import com.community.platform.dto.ServiceEvaluationPendingVO;
import com.community.platform.security.UserDetailsImpl;
import com.community.platform.service.ServiceEvaluationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 服务评价控制器
 */
@RestController
@RequestMapping("/service-evaluation")
public class ServiceEvaluationController {
    
    @Autowired
    private ServiceEvaluationService serviceEvaluationService;
    
    /**
     * 评价服务（需求发布人，普通用户）
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Result<Void> evaluateService(@Valid @RequestBody ServiceEvaluationDTO dto) {
        try {
            Long residentId = getCurrentUserId();
            serviceEvaluationService.evaluateService(residentId, dto);
            return Result.success("评价成功", null);
        } catch (Exception e) {
            return Result.error("评价失败: " + e.getMessage());
        }
    }

    /**
     * 待评价列表（当前用户为发布人的已完成认领）
     */
    @GetMapping("/my-pending")
    @PreAuthorize("hasRole('USER')")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<ServiceEvaluationPendingVO>> myPending(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            Long residentId = getCurrentUserId();
            return Result.success(serviceEvaluationService.listPending(residentId, current, size));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 已评价历史（当前用户作为评价方发出的记录）
     */
    @GetMapping("/my-history")
    @PreAuthorize("hasRole('USER')")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<ServiceEvaluationHistoryVO>> myHistory(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            Long residentId = getCurrentUserId();
            return Result.success(serviceEvaluationService.listHistory(residentId, current, size));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 我收到的评价（当前用户作为认领人/被评价方）
     */
    @GetMapping("/my-received")
    @PreAuthorize("hasRole('USER')")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<com.community.platform.dto.VolunteerEvaluationVO>> myReceived(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            Long volunteerId = getCurrentUserId();
            return Result.success(serviceEvaluationService.listReceived(volunteerId, current, size));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getUser().getId();
        }
        throw new RuntimeException("未登录");
    }
}
