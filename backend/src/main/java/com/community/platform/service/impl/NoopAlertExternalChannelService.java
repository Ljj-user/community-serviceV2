package com.community.platform.service.impl;

import com.community.platform.service.AlertExternalChannelService;
import org.springframework.stereotype.Service;

/**
 * 外部消息渠道空实现（MVP 阶段不接邮箱/短信网关）。
 */
@Service
public class NoopAlertExternalChannelService implements AlertExternalChannelService {

    @Override
    public void sendEmailAlert(Long communityId, String title, String summary) {
        // no-op
    }

    @Override
    public void sendSmsAlert(Long communityId, String title, String summary) {
        // no-op
    }
}
