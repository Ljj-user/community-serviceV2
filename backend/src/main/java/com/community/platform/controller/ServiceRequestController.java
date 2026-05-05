package com.community.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.community.platform.common.Result;
import com.community.platform.dto.ServiceRequestAuditDTO;
import com.community.platform.dto.ServiceRequestCreateDTO;
import com.community.platform.dto.ServiceRequestQueryDTO;
import com.community.platform.dto.ServiceRequestVO;
import com.community.platform.dto.ServiceMonitorQueryDTO;
import com.community.platform.dto.ServiceMonitorVO;
import com.community.platform.security.UserDetailsImpl;
import com.community.platform.service.ServiceRequestService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 需求管理控制器
 */
@RestController
@RequestMapping("/service-request")
public class ServiceRequestController {
    
    @Autowired
    private ServiceRequestService serviceRequestService;
    
    /**
     * 发布需求（居民）
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Result<ServiceRequestVO> createRequest(@Valid @RequestBody ServiceRequestCreateDTO dto) {
        try {
            Long userId = getCurrentUserId();
            ServiceRequestVO vo = serviceRequestService.createRequest(userId, dto);
            return Result.success("需求发布成功，等待审核", vo);
        } catch (Exception e) {
            return Result.error("发布失败: " + e.getMessage());
        }
    }
    
    /**
     * 审核需求（社区管理员）
     */
    @PostMapping("/audit")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<Void> auditRequest(@Valid @RequestBody ServiceRequestAuditDTO dto) {
        try {
            Long auditorId = getCurrentUserId();
            serviceRequestService.auditRequest(auditorId, dto);
            String message = dto.getApproved() ? "审核通过" : "已驳回";
            return Result.success(message, null);
        } catch (Exception e) {
            return Result.error("审核失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页查询需求列表
     */
    @GetMapping("/list")
    public Result<IPage<ServiceRequestVO>> listRequests(ServiceRequestQueryDTO queryDTO) {
        try {
            Long currentUserId = getCurrentUserId();
            IPage<ServiceRequestVO> page = serviceRequestService.listRequests(queryDTO, currentUserId);
            return Result.success(page);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询“我的需求进度”（居民）
     */
    @GetMapping("/my-list")
    @PreAuthorize("hasRole('USER')")
    public Result<IPage<ServiceRequestVO>> myList(ServiceRequestQueryDTO queryDTO) {
        try {
            Long currentUserId = getCurrentUserId();
            IPage<ServiceRequestVO> page = serviceRequestService.listMyRequests(queryDTO, currentUserId);
            return Result.success(page);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 服务过程监控列表（社区管理员）
     */
    @GetMapping("/monitor")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN', 'SUPER_ADMIN')")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<ServiceMonitorVO>> monitor(ServiceMonitorQueryDTO queryDTO) {
        try {
            Long currentUserId = getCurrentUserId();
            com.baomidou.mybatisplus.core.metadata.IPage<ServiceMonitorVO> page = serviceRequestService.listMonitor(queryDTO, currentUserId);
            return Result.success(page);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取需求详情
     */
    @GetMapping("/{id}")
    public Result<ServiceRequestVO> getRequestDetail(@PathVariable("id") Long id) {
        try {
            Long currentUserId = getCurrentUserId();
            ServiceRequestVO vo = serviceRequestService.getRequestDetail(id, currentUserId);
            return Result.success(vo);
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
