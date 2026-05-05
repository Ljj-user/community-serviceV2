package com.community.platform.dto;

import lombok.Data;

/**
 * 登录响应 DTO
 */
@Data
public class LoginResponse {
    private String token;
    private UserInfo userInfo;
}
