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
 * 社区公告表
 * </p>
 *
 * @author bishe
 * @since 2026-03-03
 */
@Getter
@Setter
@ToString
@TableName("announcement")
public class Announcement implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("title")
    private String title;

    @TableField("content_html")
    private String contentHtml;

    @TableField("content_text")
    private String contentText;

    /**
     * 推送范围：0全体 1社区 2楼栋
     */
    @TableField("target_scope")
    private Byte targetScope;

    @TableField("target_community_id")
    private Long targetCommunityId;

    @TableField("target_building_id")
    private Long targetBuildingId;

    /**
     * 状态：0草稿 1已发布
     */
    @TableField("status")
    private Byte status;

    /**
     * 是否置顶：0否 1是
     */
    @TableField("is_top")
    private Byte isTop;

    @TableField("top_at")
    private LocalDateTime topAt;

    @TableField("publisher_user_id")
    private Long publisherUserId;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    private Byte isDeleted;
}

