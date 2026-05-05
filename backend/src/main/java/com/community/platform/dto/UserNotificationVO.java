package com.community.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserNotificationVO {
    private Long id;
    private Byte msgCategory;
    private String title;
    private String summary;
    private Byte readStatus;
    private String refType;
    private Long refId;
    private LocalDateTime createdAt;
}
