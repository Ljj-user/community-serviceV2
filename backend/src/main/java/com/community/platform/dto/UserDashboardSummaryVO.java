package com.community.platform.dto;

import lombok.Data;

/**
 * 普通用户（居民/志愿者）看板汇总
 */
@Data
public class UserDashboardSummaryVO {
    /** RESIDENT 或 VOLUNTEER */
    private String panelType;
    private ResidentDashboardVO resident;
    private VolunteerDashboardVO volunteer;
}
