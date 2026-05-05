package com.community.platform.job;

import com.community.platform.service.impl.AnomalyRuleEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 异常预警规则调度任务
 */
@Component
public class AnomalyDetectorJob {

    @Autowired
    private AnomalyRuleEngineService anomalyRuleEngineService;

    /**
     * 每 10 分钟执行一次
     */
    @Scheduled(fixedDelay = 10 * 60 * 1000L, initialDelay = 90 * 1000L)
    public void run() {
        anomalyRuleEngineService.runAllRules();
    }
}
