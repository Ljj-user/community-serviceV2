package com.community.platform.common;

import java.util.List;

/**
 * 常量类
 */
public class Constants {
    
    /**
     * 角色常量
     */
    public static final Byte ROLE_SUPER_ADMIN = 1;  // 超级管理员
    public static final Byte ROLE_COMMUNITY_ADMIN = 2;  // 社区管理员
    public static final Byte ROLE_NORMAL_USER = 3;  // 普通用户

    /**
     * 普通用户身份常量（仅 role=3 时使用）：1 居民老人，2 志愿者，互斥。
     */
    public static final Byte IDENTITY_RESIDENT = 1;
    public static final Byte IDENTITY_VOLUNTEER = 2;

    /**
     * 需求状态常量
     */
    public static final Byte REQUEST_STATUS_PENDING = 0;  // 待审核
    public static final Byte REQUEST_STATUS_PUBLISHED = 1;  // 已发布
    public static final Byte REQUEST_STATUS_CLAIMED = 2;  // 已认领
    public static final Byte REQUEST_STATUS_COMPLETED = 3;  // 已完成
    public static final Byte REQUEST_STATUS_REJECTED = 4;  // 已驳回
    /**
     * 志愿者已提交完成信息，等待需求方确认（超时无异议自动完成）
     */
    public static final Byte REQUEST_STATUS_PENDING_CONFIRM = 5;  // 待确认

    /**
     * 用户状态常量
     */
    public static final Byte USER_STATUS_DISABLED = 0;  // 禁用
    public static final Byte USER_STATUS_ENABLED = 1;  // 启用
    
    /**
     * 认领状态常量
     */
    public static final Byte CLAIM_STATUS_CLAIMED = 1;  // 已认领
    public static final Byte CLAIM_STATUS_COMPLETED = 2;  // 已完成
    public static final Byte CLAIM_STATUS_CANCELLED = 3;  // 已取消
    public static final Byte CLAIM_STATUS_PENDING_CONFIRM = 4;  // 待确认
    public static final Byte CLAIM_STATUS_DISPUTED = 5;  // 已申诉（待管理员处理）

    /**
     * 站内通知分类：1业务待办 2系统公告
     */
    public static final Byte NOTIFICATION_CATEGORY_BUSINESS = 1;
    public static final Byte NOTIFICATION_CATEGORY_ANNOUNCEMENT = 2;

    public static final Byte NOTIFICATION_UNREAD = 0;
    public static final Byte NOTIFICATION_READ = 1;

    /**
     * 服务评价评价方角色：1居民（需求方） 2志愿者（服务方）
     */
    public static final Byte EVAL_ROLE_RESIDENT = 1;
    public static final Byte EVAL_ROLE_VOLUNTEER = 2;

    /**
     * 时间币结算：默认按 1 小时 = 1 时间币折算，四舍五入到最近整数，每单至少 1 币。
     */
    public static final Long TIME_COINS_PER_SERVICE_HOUR = 1L;
    public static final Long MIN_TIME_COINS_PER_COMPLETED_SERVICE = 1L;

    /**
     * 允许发布的服务类型白名单
     */
    public static final List<String> ALLOWED_SERVICE_TYPES = List.of(
            "助老服务（陪护 / 陪诊）",
            "代办服务（买菜 / 取药）",
            "家政清洁",
            "心理陪伴 / 聊天",
            "应急帮助（紧急求助）",
            "社区活动支持"
    );

    /**
     * 站内通知关联类型（ref_type）
     */
    public static final String NOTIF_REF_REQUEST_AUDIT = "REQUEST_AUDIT";
    public static final String NOTIF_REF_SERVICE_CLAIM = "SERVICE_CLAIM";
    public static final String NOTIF_REF_ANNOUNCEMENT = "ANNOUNCEMENT";
    public static final String NOTIF_REF_CARE_ALERT = "CARE_ALERT";
    public static final String NOTIF_REF_ANOMALY_ALERT = "ANOMALY_ALERT";
}
