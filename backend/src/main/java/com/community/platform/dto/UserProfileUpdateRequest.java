package com.community.platform.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 更新当前登录用户资料请求 DTO
 */
@Data
public class UserProfileUpdateRequest {
    /** 新用户名（可选；修改后 token 映射会在 Controller 层同步） */
    private String username;
    private String realName;
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    private String email;
    private String avatarUrl;
    private String address;
    /** 所属社区（网格化管理） */
    private Long communityId;
    /** 性别：0未知 1男 2女 */
    private Byte gender;
    /**
     * 志愿者能力标签，JSON 数组字符串，如 ["护理","助浴"]（仅志愿者有效）
     */
    private String skillTags;

    /** 身份标签（如普通居民、孤寡老人、残疾人），用于优先级/补贴依据 */
    private String identityTag;
}

