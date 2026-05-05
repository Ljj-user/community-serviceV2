package com.community.platform.service;

/**
 * 异常预警外部渠道（邮箱/短信）扩展点。
 * 当前阶段仅保留接口，不做真实发送。
 */
public interface AlertExternalChannelService {

    void sendEmailAlert(Long communityId, String title, String summary);

    void sendSmsAlert(Long communityId, String title, String summary);
}
