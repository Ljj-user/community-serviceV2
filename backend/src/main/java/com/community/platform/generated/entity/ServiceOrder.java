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
 * 服务订单表（用于时间银行闭环扩展）
 * </p>
 *
 * @author bishe
 * @since 2026-04-02
 */
@Getter
@Setter
@ToString
@TableName("service_order")
public class ServiceOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 需求ID（service_request）
     */
    @TableField("request_id")
    private Long requestId;

    /**
     * 服务执行志愿者（可空）
     */
    @TableField("volunteer_user_id")
    private Long volunteerUserId;

    /**
     * 需求所属社区（关联 sys_region）
     */
    @TableField("community_id")
    private Long communityId;

    /**
     * 订单状态（留作扩展）
     */
    @TableField("status")
    private Byte status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    private Byte isDeleted;
}

