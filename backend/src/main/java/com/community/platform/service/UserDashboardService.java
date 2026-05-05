package com.community.platform.service;

import com.community.platform.dto.UserDashboardSummaryVO;

/**
 * 普通用户（居民/志愿者）首页看板
 */
public interface UserDashboardService {

    UserDashboardSummaryVO buildSummary(Long userId);
}
