package com.community.platform.dto;

import lombok.Data;

/**
 * 服务过程监控查询 DTO
 */
@Data
public class ServiceMonitorQueryDTO {

    /**
     * 页码
     */
    private Integer current = 1;

    /**
     * 每页数量
     */
    private Integer size = 10;

    /**
     * 风险类型：
     * 1 - 超时未认领
     * 2 - 超时未完成
     */
    private Integer riskType;
    private Long communityId;
}

