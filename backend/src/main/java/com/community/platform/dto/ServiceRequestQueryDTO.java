package com.community.platform.dto;

import lombok.Data;

/**
 * 需求列表查询 DTO
 */
@Data
public class ServiceRequestQueryDTO {
    /**
     * 状态：0待审核 1已发布 2已认领 3已完成 4已驳回
     */
    private Byte status;
    
    /**
     * 服务类型
     */
    private String serviceType;
    
    /**
     * 紧急程度：1低 2中 3高 4紧急
     */
    private Byte urgencyLevel;
    private Long communityId;
    
    /**
     * 当前页码（默认1）
     */
    private Integer current = 1;
    
    /**
     * 每页大小（默认10）
     */
    private Integer size = 10;

    /**
     * 排序字段：createdAt / publishedAt / expectedTime / urgencyLevel
     */
    private String sortBy;

    /**
     * 排序方向：asc / desc
     */
    private String sortOrder;
}
