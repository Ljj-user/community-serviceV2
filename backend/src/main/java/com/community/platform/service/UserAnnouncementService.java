package com.community.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.community.platform.dto.AnnouncementVO;
import com.community.platform.dto.UserAnnouncementQueryDTO;

/**
 * 普通用户公告服务接口（只读）
 */
public interface UserAnnouncementService {

    IPage<AnnouncementVO> list(UserAnnouncementQueryDTO queryDTO);

    AnnouncementVO detail(Long id);
}

