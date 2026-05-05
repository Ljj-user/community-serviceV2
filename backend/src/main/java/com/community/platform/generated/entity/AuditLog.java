package com.community.platform.generated.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志表
 */
@Getter
@Setter
@TableName("audit_log")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("role")
    private Byte role;

    @TableField("module")
    private String module;

    @TableField("action")
    private String action;

    @TableField("request_path")
    private String requestPath;

    @TableField("http_method")
    private String httpMethod;

    @TableField("success")
    private Byte success;

    @TableField("result_msg")
    private String resultMsg;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("ip")
    private String ip;

    @TableField("user_agent")
    private String userAgent;

    @TableField("elapsed_ms")
    private Integer elapsedMs;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
