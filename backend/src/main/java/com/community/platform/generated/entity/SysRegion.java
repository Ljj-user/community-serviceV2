package com.community.platform.generated.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * <p>
 * 区域/网格表
 * </p>
 *
 * @author bishe
 * @since 2026-04-02
 */
@Getter
@Setter
@ToString
@TableName("sys_region")
public class SysRegion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    /**
     * 区域层级：1区、2街道、3社区
     */
    @TableField("level")
    private Byte level;

    @TableField("parent_id")
    private Long parentId;

    /** 省（展示用，如 浙江省） */
    @TableField("province")
    private String province;

    /** 市（展示用，如 杭州市） */
    @TableField("city")
    private String city;
}

