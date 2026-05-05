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
 * 服务评价表
 * </p>
 *
 * @author bishe
 * @since 2026-01-23
 */
@Getter
@Setter
@ToString
@TableName("service_evaluation")
public class ServiceEvaluation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 认领记录ID
     */
    @TableField("claim_id")
    private Long claimId;

    /**
     * 需求ID（冗余，便于查询）
     */
    @TableField("request_id")
    private Long requestId;

    /**
     * 评价人（居民）用户ID
     */
    @TableField("resident_user_id")
    private Long residentUserId;

    /**
     * 被评价人（志愿者）用户ID
     */
    @TableField("volunteer_user_id")
    private Long volunteerUserId;

    /**
     * 评价方角色：1居民（需求方）2志愿者（服务方）
     */
    @TableField("evaluator_role")
    private Byte evaluatorRole;

    /**
     * 星级：1-5
     */
    @TableField("rating")
    private Byte rating;

    /**
     * 评价内容
     */
    @TableField("content")
    private String content;

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
