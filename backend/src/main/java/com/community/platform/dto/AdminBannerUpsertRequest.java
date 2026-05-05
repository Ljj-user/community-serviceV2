package com.community.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminBannerUpsertRequest {
    /** 更新时必传；创建时可不传 */
    private Long id;

    /** 社区ID。社区管理员可不传（默认本社区）；系统管理员可传 NULL 表示全局默认 */
    private Long communityId;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String subtitle;
    private String imageUrl;
    private String linkUrl;

    @NotNull(message = "sortNo 不能为空")
    private Integer sortNo = 0;

    @NotNull(message = "status 不能为空")
    private Integer status = 1;
}

