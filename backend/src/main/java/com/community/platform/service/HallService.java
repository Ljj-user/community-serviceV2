package com.community.platform.service;

import com.community.platform.dto.HallSummaryVO;

/**
 * 大厅业务服务
 */
public interface HallService {

    /**
     * 获取“服务概览”汇总数据
     */
    HallSummaryVO getSummary(Long userId);
}

