package com.community.platform.job;

import com.community.platform.service.impl.ServiceClaimServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ServiceAutoCompleteJob {

    @Autowired
    private ServiceClaimServiceImpl serviceClaimService;

    /**
     * 每 10 分钟扫描一次，24 小时无异议自动完成。
     */
    @Scheduled(fixedDelay = 10 * 60 * 1000L, initialDelay = 120 * 1000L)
    public void run() {
        serviceClaimService.autoCompletePendingClaims(24);
    }
}
