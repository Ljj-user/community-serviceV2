package com.community.platform.dto;

import lombok.Data;

@Data
public class NotificationUnreadCountVO {
    private long total;
    private long business;
    private long announcement;
}
