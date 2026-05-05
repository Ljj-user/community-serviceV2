package com.community.platform.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 资金/物资流动监控（超级管理员大盘占位；后续可对接财务/物资子系统）
 */
@Data
public class FundingMonitorVO {
    private BigDecimal fundIn;
    private BigDecimal fundOut;
    private BigDecimal materialIn;
    private BigDecimal materialOut;
    private String note;
}
