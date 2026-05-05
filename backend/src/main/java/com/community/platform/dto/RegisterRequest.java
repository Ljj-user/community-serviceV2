package com.community.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.List;

/**
 * 注册请求 DTO
 */
@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 角色：1超级管理员 2社区管理员 3普通用户（注册时后端强制为 3，忽略客户端）
     */
    private Byte role = 3;
    
    /**
     * 普通用户身份：1居民老人 2志愿者（仅 role=3，互斥）
     */
    @NotNull(message = "请选择身份")
    private Byte identityType;

    /**
     * 所属社区（网格化管理，可选）
     */
    private Long communityId;

    /**
     * 性别：0未知 1男 2女（用于默认头像；可选，默认按未知处理）
     */
    private Byte gender;

    /**
     * 技能标签（可选）：用于新用户冷启动推荐画像，如 ["陪诊","家电维修"]。
     */
    private List<String> skillTags;

    @NotBlank(message = "验证码不能为空")
    private String verificationCode;

    /** 验证码场景，默认 REGISTER */
    private String verificationScene = "REGISTER";
}
