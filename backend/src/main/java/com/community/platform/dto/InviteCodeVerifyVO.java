package com.community.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InviteCodeVerifyVO {
    private Long communityId;
    private String communityName;
    private LocalDateTime expiresAt;
    private Integer maxUses;
    private Integer usedCount;
}

