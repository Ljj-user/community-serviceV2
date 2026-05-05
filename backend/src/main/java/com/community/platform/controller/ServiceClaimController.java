package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.dto.ServiceClaimDTO;
import com.community.platform.dto.ServiceCompleteDTO;
import com.community.platform.dto.ServiceConfirmDTO;
import com.community.platform.dto.ServiceDisputeDTO;
import com.community.platform.security.UserDetailsImpl;
import com.community.platform.service.ServiceClaimService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

/**
 * 服务认领控制器
 */
@RestController
@RequestMapping("/service-claim")
public class ServiceClaimController {
    
    @Autowired
    private ServiceClaimService serviceClaimService;
    
    /**
     * 认领服务（志愿者）
     */
    @PostMapping("/claim")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> claimService(@Valid @RequestBody ServiceClaimDTO dto) {
        try {
            Long volunteerId = getCurrentUserId();
            serviceClaimService.claimService(volunteerId, dto);
            return Result.success("认领成功", null);
        } catch (Exception e) {
            return Result.error("认领失败: " + e.getMessage());
        }
    }
    
    /**
     * 完成服务（志愿者提交时长）
     */
    @PostMapping("/complete")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> completeService(@Valid @RequestBody ServiceCompleteDTO dto) {
        try {
            Long volunteerId = getCurrentUserId();
            serviceClaimService.completeService(volunteerId, dto);
            return Result.success("服务已提交，等待需求方确认（24小时无异议将自动完成）", null);
        } catch (Exception e) {
            return Result.error("完成失败: " + e.getMessage());
        }
    }

    /**
     * 需求方核销确认（触发时间币结算）
     */
    @PostMapping("/confirm")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> confirm(@Valid @RequestBody ServiceConfirmDTO dto) {
        try {
            Long requesterUserId = getCurrentUserId();
            serviceClaimService.confirmService(requesterUserId, dto);
            return Result.success("核销成功，时间币已结算", null);
        } catch (Exception e) {
            return Result.error("核销失败: " + e.getMessage());
        }
    }

    /**
     * 需求方申诉（防作弊反馈）
     */
    @PostMapping("/dispute")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> dispute(@Valid @RequestBody ServiceDisputeDTO dto) {
        try {
            Long requesterUserId = getCurrentUserId();
            serviceClaimService.disputeService(requesterUserId, dto);
            return Result.success("已提交申诉，社区管理员将介入处理", null);
        } catch (Exception e) {
            return Result.error("申诉失败: " + e.getMessage());
        }
    }

    @PostMapping("/dispute-by-request/{requestId}")
    @PreAuthorize("hasRole('USER')")
    public Result<Void> disputeByRequest(@PathVariable("requestId") Long requestId,
                                         @RequestParam("reason") String reason) {
        try {
            if (!StringUtils.hasText(reason)) {
                return Result.error("申诉内容不能为空");
            }
            Long requesterUserId = getCurrentUserId();
            serviceClaimService.disputeByRequest(requesterUserId, requestId, reason.trim());
            return Result.success("已提交申诉，社区管理员将介入处理", null);
        } catch (Exception e) {
            return Result.error("申诉失败: " + e.getMessage());
        }
    }

    /**
     * 一键提醒（社区管理员人工干预使用）
     * 这里只做占位，实际提醒逻辑可接入短信/通知中心
     */
    @PostMapping("/remind/{requestId}")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<Void> remindForRequest(@PathVariable("requestId") Long requestId) {
        // 当前实现仅返回成功提示，实际业务可记录提醒日志
        return Result.success("已触发提醒，后续可接入短信/站内信等通知渠道", null);
    }
    
    /**
     * 获取我的服务记录（志愿者）
     */
    @GetMapping("/my-records")
    @PreAuthorize("hasRole('USER')")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<com.community.platform.dto.ServiceClaimVO>> getMyServiceRecords(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "claimStatus", required = false) Byte claimStatus,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortOrder", required = false) String sortOrder) {
        try {
            Long volunteerId = getCurrentUserId();
            com.baomidou.mybatisplus.core.metadata.IPage<com.community.platform.dto.ServiceClaimVO> page = 
                    serviceClaimService.getMyServiceRecords(volunteerId, current, size, claimStatus, sortBy, sortOrder);
            return Result.success(page);
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
