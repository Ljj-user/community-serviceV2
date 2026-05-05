package com.community.platform.dto;

import lombok.Data;

/**
 * 审计日志列表查询参数（与前端 AuditLogListParams 对齐）
 */
@Data
public class AuditLogListQuery {
    private Integer page = 1;
    private Integer size = 10;
    private String username;
    private Byte role;
    private String module;
    private String action;
    private Byte success;
    private String riskLevel;
    private String startTime;  // ISO 字符串
    private String endTime;    // ISO 字符串
}
