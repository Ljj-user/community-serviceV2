package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.dto.AiChatRequest;
import com.community.platform.dto.AiChatResponseVO;
import com.community.platform.dto.AiAnalysisRecordVO;
import com.community.platform.dto.PageResult;
import com.community.platform.service.impl.AiChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.community.platform.security.UserDetailsImpl;

@RestController
@RequestMapping("/ai")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('USER','COMMUNITY_ADMIN','SUPER_ADMIN')")
    public Result<AiChatResponseVO> chat(@Valid @RequestBody AiChatRequest request) {
        try {
            return Result.success(aiChatService.chat(getCurrentUserId(), request.getMessage(), request.getHistory()));
        } catch (Exception e) {
            return Result.error("AI 对话失败: " + e.getMessage());
        }
    }

    @GetMapping("/records/mine")
    @PreAuthorize("hasAnyRole('USER','COMMUNITY_ADMIN','SUPER_ADMIN')")
    public Result<PageResult<AiAnalysisRecordVO>> myRecords(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        try {
            return Result.success(aiChatService.listMine(getCurrentUserId(), page, size));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/records/{id}/apply")
    @PreAuthorize("hasAnyRole('USER','COMMUNITY_ADMIN','SUPER_ADMIN')")
    public Result<Void> markApplied(@PathVariable("id") Long id) {
        try {
            aiChatService.markApplied(getCurrentUserId(), id);
            return Result.success("已记录为带入表单", null);
        } catch (Exception e) {
            return Result.error("记录失败: " + e.getMessage());
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getUser().getId();
        }
        throw new RuntimeException("未登录");
    }
}
