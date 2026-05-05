package com.community.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 移动端预警卡片字段规范
 */
@Data
public class MobileAlertCardVO {
    private Long id;
    private String alertType;
    private String alertTypeLabel;
    private Integer severityLevel;
    private String severityLabel;
    private String severityColor;
    private String title;
    private String summary;
    private Byte readStatus;
    private LocalDateTime createdAt;
    private String actionType;
    private String actionTarget;
    private String refType;
    private Long refId;
}
