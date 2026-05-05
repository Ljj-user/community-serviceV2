package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.dto.*;
import com.community.platform.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 超级管理员：用户与角色管理
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @GetMapping("/list")
    public Result<PageResult<AdminUserVO>> list(AdminUserListQuery query) {
        return Result.success(adminUserService.list(query));
    }

    @PostMapping
    public Result<AdminUserVO> create(@Valid @RequestBody AdminUserCreateRequest request) {
        try {
            return Result.success("创建成功", adminUserService.create(request));
        } catch (Exception e) {
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    @PutMapping
    public Result<AdminUserVO> update(@Valid @RequestBody AdminUserUpdateRequest request) {
        try {
            return Result.success("更新成功", adminUserService.update(request));
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        try {
            adminUserService.delete(id);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/status")
    public Result<AdminUserVO> setStatus(@PathVariable("id") Long id,
                                         @RequestParam("status") Byte status) {
        try {
            return Result.success("更新成功", adminUserService.setStatus(id, status));
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }
}

