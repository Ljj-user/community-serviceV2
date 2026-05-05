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
 * 站内消息通知
 */
@Getter
@Setter
@ToString
@TableName("sys_notification")
public class SysNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("recipient_user_id")
    private Long recipientUserId;

    @TableField("title")
    private String title;

    @TableField("summary")
    private String summary;

    @TableField("msg_category")
    private Byte msgCategory;

    @TableField("read_status")
    private Byte readStatus;

    @TableField("ref_type")
    private String refType;

    @TableField("ref_id")
    private Long refId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
