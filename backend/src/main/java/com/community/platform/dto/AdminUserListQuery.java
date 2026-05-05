package com.community.platform.dto;

import lombok.Data;

/**
 * 超级管理员用户列表查询参数
 */
@Data
public class AdminUserListQuery {
    private String username;
    private Byte role;
    private Byte status;
    private Long communityId;
    private Integer page = 1;
    private Integer size = 10;
}

