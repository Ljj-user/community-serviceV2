package com.community.platform.generated.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 备份/恢复/导出记录表
 */
@Getter
@Setter
@TableName("backup_record")
public class BackupRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("record_type")
    private String recordType;

    @TableField("module")
    private String module;

    @TableField("format")
    private String format;

    @TableField("filename")
    private String filename;

    @TableField("file_path")
    private String filePath;

    @TableField("file_size_mb")
    private BigDecimal fileSizeMb;

    @TableField("status")
    private String status;

    @TableField("note")
    private String note;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    private Byte isDeleted;
}

