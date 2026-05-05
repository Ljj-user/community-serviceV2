package com.community.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VerificationCodeTicketVO {
    private Long ticketId;
    private String scene;
    private String target;
    private LocalDateTime expiresAt;
    /** 仅在 app.auth.expose-dev-code=true 时返回验证码 */
    private String devCode;
}

