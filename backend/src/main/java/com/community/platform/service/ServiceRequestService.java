package com.community.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.dto.ServiceRequestAuditDTO;
import com.community.platform.dto.ServiceRequestCreateDTO;
import com.community.platform.dto.ServiceRequestQueryDTO;
import com.community.platform.dto.ServiceRequestVO;
import com.community.platform.dto.ServiceMonitorQueryDTO;
import com.community.platform.dto.ServiceMonitorVO;

/**
 * 需求管理服务接口
 */
public interface ServiceRequestService {
    
    /**
     * 发布需求（居民）
     */
    ServiceRequestVO createRequest(Long userId, ServiceRequestCreateDTO dto);
    
    /**
     * 审核需求（社区管理员）
     */
    void auditRequest(Long auditorId, ServiceRequestAuditDTO dto);
    
    /**
     * 分页查询需求列表
     */
    IPage<ServiceRequestVO> listRequests(ServiceRequestQueryDTO queryDTO, Long currentUserId);

    /**
     * 分页查询“我的需求”（仅当前用户自己发布的需求）
     */
    IPage<ServiceRequestVO> listMyRequests(ServiceRequestQueryDTO queryDTO, Long currentUserId);
    
    /**
     * 获取需求详情
     */
    ServiceRequestVO getRequestDetail(Long requestId, Long currentUserId);

    /**
     * 服务过程监控列表
     */
    IPage<ServiceMonitorVO> listMonitor(ServiceMonitorQueryDTO queryDTO, Long currentUserId);
}
