package com.community.platform.generated.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.platform.generated.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统配置 Mapper
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    SysConfig selectByConfigKey(@Param("configKey") String configKey);
}
