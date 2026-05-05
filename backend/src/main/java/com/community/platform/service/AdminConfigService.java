package com.community.platform.service;

import java.util.Map;

/**
 * 系统配置服务
 */
public interface AdminConfigService {

    Map<String, Object> getBasic();
    void saveBasic(Map<String, Object> data);

    Map<String, Object> getNotice();
    void saveNotice(Map<String, Object> data);

    Map<String, Object> getAlert();
    void saveAlert(Map<String, Object> data);

    Map<String, Object> getAi();
    void saveAi(Map<String, Object> data);

    Map<String, Object> testAi(Map<String, Object> data);

    Map<String, Object> getRuntime();
    void saveRuntime(Map<String, Object> data);
}
