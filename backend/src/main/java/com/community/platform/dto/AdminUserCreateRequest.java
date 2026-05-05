package com.community.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 超级管理员创建用户请求 DTO
 */
@Data
public class AdminUserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "初始密码不能为空")
    private String password;

    /**
     * 角色：1超级管理员 2社区管理员 3普通用户
     */
    @NotNull(message = "角色不能为空")
    private Byte role;

    /**
     * 普通用户身份：1居民老人 2志愿者（仅 role=3，互斥）
     */
    private Byte identityType;

    private String realName;
    private String phone;
    private String email;
    private String address;

    /**
     * 状态：0禁用 1启用
     */
    private Byte status;
}

