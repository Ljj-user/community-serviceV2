package com.community.platform.dto;

import lombok.Data;

/**
 * 备份计划任务配置 DTO（与前端 schedule 对齐）
 */
@Data
public class BackupScheduleDTO {
    private Boolean enabled;
    private String cycle;   // daily/weekly/monthly
    private String time;    // HH:mm
    private Integer keepDays;
}

