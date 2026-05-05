package com.community.platform.service;

import com.community.platform.dto.ServiceEvaluationDTO;
import com.community.platform.dto.ServiceEvaluationHistoryVO;
import com.community.platform.dto.ServiceEvaluationPendingVO;
import com.community.platform.dto.VolunteerEvaluationVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 服务评价服务接口
 */
public interface ServiceEvaluationService {
    
    /**
     * 评价服务（居民）
     */
    void evaluateService(Long residentId, ServiceEvaluationDTO dto);

    /**
     * 获取待评价列表（居民）
     */
    IPage<ServiceEvaluationPendingVO> listPending(Long residentId, Integer current, Integer size);

    /**
     * 获取已评价历史（居民）
     */
    IPage<ServiceEvaluationHistoryVO> listHistory(Long residentId, Integer current, Integer size);

    /**
     * 志愿者收到的评价列表
     */
    IPage<VolunteerEvaluationVO> listReceived(Long volunteerId, Integer current, Integer size);
}
