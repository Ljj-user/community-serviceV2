package com.community.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志 VO（与前端 AuditLogVO 对齐）
 */
@Data
public class AuditLogVO {
    private Long id;
    private Long userId;
    private String username;
    private Byte role;
    private String module;
    private String action;
    private String requestPath;
    private String httpMethod;
    private Byte success;
    private String resultMsg;
    private String riskLevel;
    private String ip;
    private String userAgent;
    private Integer elapsedMs;
    private LocalDateTime createdAt;
}
