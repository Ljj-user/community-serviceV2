package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.dto.AiAnalysisRecordVO;
import com.community.platform.dto.PageResult;
import com.community.platform.service.impl.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ai-analysis")
public class AdminAiAnalysisController {

    @Autowired
    private AiChatService aiChatService;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN','SUPER_ADMIN')")
    public Result<PageResult<AiAnalysisRecordVO>> list(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        try {
            return Result.success(aiChatService.listAdmin(page, size));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMMUNITY_ADMIN','SUPER_ADMIN')")
    public Result<AiAnalysisRecordVO> detail(@PathVariable("id") Long id) {
        try {
            return Result.success(aiChatService.detailAdmin(id));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
