package com.community.platform.dto;

import lombok.Data;

/**
 * 区域服务统计 VO（用于全局数据看板热力图）
 */
@Data
public class RegionStatVO {

    /**
     * 区域代码（如 ISO 国家/省份代码，供前端地图使用）
     */
    private String regionCode;

    /**
     * 服务次数或覆盖度统计值
     */
    private Long serviceCount;
}

