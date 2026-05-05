package com.community.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminInviteCodeVO {
    private Long id;
    private Long communityId;
    private String communityName;
    private String code;
    private Integer status;
    private LocalDateTime expiresAt;
    private Integer maxUses;
    private Integer usedCount;
    private LocalDateTime createdAt;
}

