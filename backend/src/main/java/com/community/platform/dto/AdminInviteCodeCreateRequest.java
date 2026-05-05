package com.community.platform.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 管理端生成邀请码
 */
@Data
public class AdminInviteCodeCreateRequest {
    /**
     * 社区ID。社区管理员可不传（默认取自身 community_id）；系统管理员必须传。
     */
    private Long communityId;

    /**
     * 有效期天数（NULL/<=0 表示不过期）
     */
    private Integer expiresInDays;

    @Min(value = 1, message = "maxUses 需大于 0")
    private Integer maxUses = 100;
}

