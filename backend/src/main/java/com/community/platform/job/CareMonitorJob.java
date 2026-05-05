package com.community.platform.job;

import com.community.platform.service.impl.AnomalyRuleEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 兼容旧任务入口：主动关怀规则由统一异常引擎执行
 */
@Component
public class CareMonitorJob {

    @Autowired
    private AnomalyRuleEngineService anomalyRuleEngineService;

    /**
     * 每 6 小时补偿执行一次
     */
    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void run() {
        anomalyRuleEngineService.runInactiveCareRule();
    }
}

