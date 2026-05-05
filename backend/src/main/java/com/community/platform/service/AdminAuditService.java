package com.community.platform.service;

import com.community.platform.dto.AuditLogVO;
import com.community.platform.dto.PageResult;
import com.community.platform.dto.AuditLogListQuery;

/**
 * 审计日志服务
 */
public interface AdminAuditService {

    PageResult<AuditLogVO> list(AuditLogListQuery query);

    AuditLogVO getById(Long id);

    /**
     * 记录一条审计日志（可在登录、用户管理、配置保存等处调用）
     */
    void record(Long userId, String username, Byte role, String module, String action,
                String requestPath, String httpMethod, boolean success, String resultMsg,
                String riskLevel, String ip, String userAgent, Integer elapsedMs);
}
