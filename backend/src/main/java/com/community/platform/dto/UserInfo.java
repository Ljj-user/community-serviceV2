package com.community.platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息 DTO（返回给前端，不包含敏感信息）
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class UserInfo {
    private Long id;
    private String username;
    private Byte role;
    private Byte identityType;
    /** 绑定社区区域 ID（sys_region.id） */
    private Long communityId;
    /** 社区加入状态：0未加入 1待审核 2已加入 3已拒绝 */
    private Byte communityJoinStatus;
    /** 绑定社区名称（由 community_id 关联 sys_region 查询） */
    private String communityName;
    /** 省（展示用，来自 sys_region） */
    private String province;
    /** 市（展示用，来自 sys_region） */
    private String city;
    /** 当前可用时间币 */
    private Long timeCoins;
    /** 累计积分 */
    private Long points;
    private String realName;
    private String phone;
    private String email;
    private String avatarUrl;
    private Byte gender;
    private String address;
    private String skillTags;
    private Byte status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
