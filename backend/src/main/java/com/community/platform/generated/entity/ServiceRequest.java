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
 * 公益服务需求表
 * </p>
 *
 * @author bishe
 * @since 2026-01-23
 */
@Getter
@Setter
@ToString
@TableName("service_request")
public class ServiceRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 需求发起人（居民）用户ID
     */
    @TableField("requester_user_id")
    private Long requesterUserId;

    /**
     * 所属社区ID（网格化隔离）
     */
    @TableField("community_id")
    private Long communityId;

    /**
     * 服务类型（如助老、清洁、教育等）
     */
    @TableField("service_type")
    private String serviceType;

    /**
     * 需求描述/补充说明
     */
    @TableField("description")
    private String description;

    /**
     * 服务地址
     */
    @TableField("service_address")
    private String serviceAddress;

    /**
     * 期望服务时间
     */
    @TableField("expected_time")
    private LocalDateTime expectedTime;

    /**
     * 紧急程度：1低 2中 3高 4紧急
     */
    @TableField("urgency_level")
    private Byte urgencyLevel;

    /**
     * 紧急联系人姓名
     */
    @TableField("emergency_contact_name")
    private String emergencyContactName;

    /**
     * 紧急联系人电话
     */
    @TableField("emergency_contact_phone")
    private String emergencyContactPhone;

    /**
     * 与服务对象关系（子女/邻居等）
     */
    @TableField("emergency_contact_relation")
    private String emergencyContactRelation;

    /**
     * 特殊人群/需求标签（JSON数组，如独居老人、残障等）
     */
    @TableField("special_tags")
    private String specialTags;

    /**
     * 状态：0待审核 1已发布 2已认领 3已完成 4已驳回
     */
    @TableField("status")
    private Byte status;

    /**
     * 审核人（社区管理员）用户ID
     */
    @TableField("audit_by_user_id")
    private Long auditByUserId;

    /**
     * 审核时间
     */
    @TableField("audit_at")
    private LocalDateTime auditAt;

    /**
     * 驳回原因（status=4时必填）
     */
    @TableField("reject_reason")
    private String rejectReason;

    /**
     * 发布公开时间（status=1）
     */
    @TableField("published_at")
    private LocalDateTime publishedAt;

    /**
     * 被认领时间（status=2）
     */
    @TableField("claimed_at")
    private LocalDateTime claimedAt;

    /**
     * 完成时间（status=3）
     */
    @TableField("completed_at")
    private LocalDateTime completedAt;

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
