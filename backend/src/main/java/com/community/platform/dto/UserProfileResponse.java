package com.community.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前登录用户资料返回 DTO
 */
@Data
public class UserProfileResponse {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String avatarUrl;
    private Byte role;
    private Byte identityType;
    /** 所属社区（网格化管理） */
    private Long communityId;
    /** 所属社区名称（与 community_id 对应 sys_region.id） */
    private String communityName;
    /** 省（展示用） */
    private String province;
    /** 市（展示用） */
    private String city;
    /** 当前可用时间币 */
    private Long timeCoins;
    /** 累计积分/经验值 */
    private Long points;
    /** 身份标签（如普通居民、孤寡老人、残疾人） */
    private String identityTag;
    /**
     * 性别：0未知 1男 2女
     */
    private Byte gender;
    /**
     * 志愿者能力标签 JSON 字符串
     */
    private String skillTags;
    private String address;
    /**
     * 账号创建时间
     */
    private LocalDateTime createdAt;
}

