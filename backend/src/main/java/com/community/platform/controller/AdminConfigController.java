package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.service.AdminConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 超级管理员：系统配置管理
 */
@RestController
@RequestMapping("/admin/config")
public class AdminConfigController {

    @Autowired
    private AdminConfigService adminConfigService;

    @GetMapping("/basic")
    public Result<Map<String, Object>> getBasic() {
        return Result.success(adminConfigService.getBasic());
    }

    @PutMapping("/basic")
    public Result<Void> saveBasic(@RequestBody Map<String, Object> data) {
        adminConfigService.saveBasic(data);
        return Result.success("基础参数保存成功", null);
    }

    @GetMapping("/notice")
    public Result<Map<String, Object>> getNotice() {
        return Result.success(adminConfigService.getNotice());
    }

    @PutMapping("/notice")
    public Result<Void> saveNotice(@RequestBody Map<String, Object> data) {
        adminConfigService.saveNotice(data);
        return Result.success("通知模板保存成功", null);
    }

    @GetMapping("/alert")
    public Result<Map<String, Object>> getAlert() {
        return Result.success(adminConfigService.getAlert());
    }

    @PutMapping("/alert")
    public Result<Void> saveAlert(@RequestBody Map<String, Object> data) {
        adminConfigService.saveAlert(data);
        return Result.success("预警规则保存成功", null);
    }

    @GetMapping("/ai")
    public Result<Map<String, Object>> getAi() {
        return Result.success(adminConfigService.getAi());
    }

    @PutMapping("/ai")
    public Result<Void> saveAi(@RequestBody Map<String, Object> data) {
        adminConfigService.saveAi(data);
        return Result.success("AI 配置保存成功", null);
    }

    @PostMapping("/ai/test")
    public Result<Map<String, Object>> testAi(@RequestBody Map<String, Object> data) {
        return Result.success(adminConfigService.testAi(data));
    }

    @GetMapping("/runtime")
    public Result<Map<String, Object>> getRuntime() {
        return Result.success(adminConfigService.getRuntime());
    }

    @PutMapping("/runtime")
    public Result<Void> saveRuntime(@RequestBody Map<String, Object> data) {
        adminConfigService.saveRuntime(data);
        return Result.success("运行态配置保存成功", null);
    }
}
