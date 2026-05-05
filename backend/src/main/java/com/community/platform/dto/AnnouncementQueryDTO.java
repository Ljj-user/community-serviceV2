package com.community.platform.dto;

import lombok.Data;

/**
 * 公告列表查询 DTO
 */
@Data
public class AnnouncementQueryDTO {
    private Integer current = 1;
    private Integer size = 10;

    /**
     * 关键字（标题）
     */
    private String keyword;

    /**
     * 推送范围：0全体 1社区 2楼栋
     */
    private Byte targetScope;

    private Long targetCommunityId;

    private Long targetBuildingId;

    /**
     * 状态：0草稿 1已发布
     */
    private Byte status;

    /**
     * 是否置顶：0否 1是
     */
    private Byte isTop;
}

