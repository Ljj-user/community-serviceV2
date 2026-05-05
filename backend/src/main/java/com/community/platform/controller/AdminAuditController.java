package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.dto.AuditLogListQuery;
import com.community.platform.dto.AuditLogVO;
import com.community.platform.dto.PageResult;
import com.community.platform.service.AdminAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 超级管理员：审核日志与审计
 */
@RestController
@RequestMapping("/admin/audit")
public class AdminAuditController {

    @Autowired
    private AdminAuditService adminAuditService;

    @GetMapping("/logs")
    public Result<PageResult<AuditLogVO>> list(AuditLogListQuery query) {
        return Result.success(adminAuditService.list(query));
    }

    @GetMapping("/logs/{id}")
    public Result<AuditLogVO> getById(@PathVariable("id") Long id) {
        AuditLogVO vo = adminAuditService.getById(id);
        if (vo == null) {
            return Result.error("记录不存在");
        }
        return Result.success(vo);
    }
}
