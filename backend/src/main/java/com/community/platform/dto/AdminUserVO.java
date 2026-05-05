package com.community.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 超级管理员用户列表展示 VO
 */
@Data
public class AdminUserVO {
    private Long id;
    private String username;
    private Byte role;
    private Byte identityType;
    private String realName;
    private String phone;
    private String email;
    private String avatarUrl;
    private Byte status;
    private Long communityId;
    private String communityName;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}

