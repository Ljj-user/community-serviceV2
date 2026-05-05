package com.community.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务排期简要行（期望服务时间在未来、待处理/进行中的需求）
 */
@Data
public class ScheduleBriefVO {
    private Long id;
    private String serviceType;
    private LocalDateTime expectedTime;
    private String serviceAddress;
    private Integer status;
}
