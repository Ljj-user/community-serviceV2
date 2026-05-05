package com.community.platform.dto;

import lombok.Data;

/**
 * 普通用户公告列表查询 DTO
 */
@Data
public class UserAnnouncementQueryDTO {
    private Integer current = 1;
    private Integer size = 10;
    private String keyword;
}

