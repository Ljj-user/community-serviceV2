package com.community.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告 VO
 */
@Data
public class AnnouncementVO {
    private Long id;
    private String title;
    private String contentHtml;

    private Byte targetScope;
    private Long targetCommunityId;
    private String targetCommunityName;
    private Long targetBuildingId;

    private Byte status;
    private Byte isTop;
    private LocalDateTime topAt;

    private Long publisherUserId;
    private String publisherName;
    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

