package com.community.platform.generated.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户表（含角色/身份）
 * </p>
 *
 * @author bishe
 * @since 2026-01-23
 */
@Getter
@Setter
@ToString
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名（登录名）
     */
    @TableField("username")
    private String username;

    /**
     * 密码哈希（字段名沿用 password_md5，值可为 BCrypt/历史 MD5）
     */
    @TableField("password_md5")
    private String passwordMd5;

    /**
     * 角色：1超级管理员 2社区管理员 3普通用户
     */
    @TableField("role")
    private Byte role;

    /**
     * 普通用户身份：1居民老人 2志愿者（仅 role=3，互斥）
     */
    @TableField("identity_type")
    private Byte identityType;

    /**
     * 关联区域ID（网格化管理）
     */
    @TableField("community_id")
    private Long communityId;

    /**
     * 社区加入状态：0未加入 1待审核 2已加入 3已拒绝
     */
    @TableField("community_join_status")
    private Byte communityJoinStatus;

    /**
     * 当前可用时间币
     */
    @TableField("time_coins")
    private Long timeCoins;

    /**
     * 累计积分/经验值
     */
    @TableField("points")
    private Long points;

    /**
     * 身份标签（如普通居民、孤寡老人、残疾人等）
     */
    @TableField("identity_tag")
    private String identityTag;

    /**
     * 真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 头像URL
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 性别：0未知 1男 2女
     */
    @TableField("gender")
    private Byte gender;

    /**
     * 常住地址/社区地址（用于匹配/服务记录）
     */
    @TableField("address")
    private String address;

    /**
     * 志愿者能力/标签（用于智能匹配；如护理、助浴等）
     */
    @TableField("skill_tags")
    private String skillTags;

    /**
     * 状态：0禁用 1启用
     */
    @TableField("status")
    private Byte status;

    /**
     * 最近登录时间
     */
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除：0否 1是
     */
    @TableField("is_deleted")
    private Byte isDeleted;
}
