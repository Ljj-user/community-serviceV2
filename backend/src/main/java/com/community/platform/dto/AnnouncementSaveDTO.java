package com.community.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 公告新增/编辑 DTO
 */
@Data
public class AnnouncementSaveDTO {

    private Long id;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String contentHtml;

    /**
     * 推送范围：0全体 1社区 2楼栋
     */
    @NotNull(message = "推送范围不能为空")
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

