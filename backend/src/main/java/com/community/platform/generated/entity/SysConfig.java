package com.community.platform.generated.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 系统配置表
 */
@Getter
@Setter
@TableName(value = "sys_config", autoResultMap = true)
public class SysConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("config_key")
    private String configKey;

    @TableField(value = "config_value", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> configValue;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
