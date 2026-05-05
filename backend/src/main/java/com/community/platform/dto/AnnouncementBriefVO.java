package com.community.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnnouncementBriefVO {
    private Long id;
    private String title;
    private LocalDateTime publishedAt;
}
