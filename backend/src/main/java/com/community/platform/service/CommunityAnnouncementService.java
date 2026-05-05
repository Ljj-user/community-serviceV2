package com.community.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.community.platform.dto.AnnouncementQueryDTO;
import com.community.platform.dto.AnnouncementSaveDTO;
import com.community.platform.dto.AnnouncementVO;

/**
 * 社区公告服务接口（社区管理员）
 */
public interface CommunityAnnouncementService {

    IPage<AnnouncementVO> list(Long operatorUserId, AnnouncementQueryDTO queryDTO);

    AnnouncementVO detail(Long operatorUserId, Long id);

    Long save(Long publisherUserId, AnnouncementSaveDTO dto);

    void delete(Long id, Long operatorUserId);

    void setTop(Long id, boolean isTop, Long operatorUserId);
}

