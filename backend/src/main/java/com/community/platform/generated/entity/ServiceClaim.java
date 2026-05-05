package com.community.platform.generated.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务认领记录表
 * </p>
 *
 * @author bishe
 * @since 2026-01-23
 */
@Getter
@Setter
@ToString
@TableName("service_claim")
public class ServiceClaim implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 需求ID
     */
    @TableField("request_id")
    private Long requestId;

    /**
     * 志愿者用户ID
     */
    @TableField("volunteer_user_id")
    private Long volunteerUserId;

    /**
     * 认领时间
     */
    @TableField("claim_at")
    private LocalDateTime claimAt;

    /**
     * 状态：1已认领 2已完成 3已取消
     */
    @TableField("claim_status")
    private Byte claimStatus;

    /**
     * 服务时长（小时，完成后提交）
     */
    @TableField("service_hours")
    private BigDecimal serviceHours;

    /**
     * 时长提交时间
     */
    @TableField("hours_submitted_at")
    private LocalDateTime hoursSubmittedAt;

    /**
     * 完成说明/备注（可用于异常记录）
     */
    @TableField("completion_note")
    private String completionNote;

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
