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
 * 时间币流水表（时间银行）
 * </p>
 *
 * @author bishe
 * @since 2026-04-02
 */
@Getter
@Setter
@ToString
@TableName("time_transaction")
public class TimeTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 变动额度（可正可负）
     */
    @TableField("amount")
    private Long amount;

    /**
     * 变动类型：1-服务所得 2-兑换消耗 3-系统补贴
     */
    @TableField("type")
    private Byte type;

    /**
     * 关联订单ID（service_order）
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}

