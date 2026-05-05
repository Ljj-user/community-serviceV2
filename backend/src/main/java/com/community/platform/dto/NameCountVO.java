package com.community.platform.dto;

import lombok.Data;

/**
 * 名称 + 计数（用于需求类型分布等）
 */
@Data
public class NameCountVO {
    private String name;
    private Long count;
}
