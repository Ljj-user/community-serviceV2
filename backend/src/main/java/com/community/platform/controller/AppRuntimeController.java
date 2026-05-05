package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.dto.AppRuntimeVO;
import com.community.platform.service.AdminConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/app")
public class AppRuntimeController {

    @Autowired
    private AdminConfigService adminConfigService;

    @GetMapping("/runtime")
    public Result<AppRuntimeVO> runtime() {
        Map<String, Object> runtime = adminConfigService.getRuntime();
        AppRuntimeVO vo = new AppRuntimeVO();
        boolean enabled = true;
        if (runtime.containsKey("demoModeEnabled")) {
            enabled = toBool(runtime.get("demoModeEnabled"));
        }
        vo.setDemoModeEnabled(enabled);
        vo.setDemoModeLabel(enabled ? "演示环境" : "正式风格环境");
        vo.setDemoDataHint("当前环境已预置社区、求助单、志愿者认证、公告、便民信息、预警和 AI 记录。");
        return Result.success(vo);
    }

    private boolean toBool(Object raw) {
        if (raw instanceof Boolean b) return b;
        if (raw == null) return false;
        String s = String.valueOf(raw).trim();
        return "1".equals(s) || "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s);
    }
}
