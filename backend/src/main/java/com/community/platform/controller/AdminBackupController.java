package com.community.platform.controller;

import com.community.platform.common.Result;
import com.community.platform.dto.BackupScheduleDTO;
import com.community.platform.dto.PageResult;
import com.community.platform.generated.entity.BackupRecord;
import com.community.platform.service.AdminBackupService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 超级管理员：数据导出与备份
 */
@RestController
@RequestMapping("/admin/backup")
public class AdminBackupController {

    @Autowired
    private AdminBackupService adminBackupService;

    @PostMapping("/run")
    public Result<BackupRecord> runBackup() {
        try {
            return Result.success("备份完成", adminBackupService.runBackup());
        } catch (Exception e) {
            return Result.error("备份失败: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public Result<PageResult<BackupRecord>> history(@RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
                                                   @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        return Result.success(adminBackupService.history(page, size));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        try {
            adminBackupService.deleteRecord(id);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @GetMapping("/schedule")
    public Result<BackupScheduleDTO> getSchedule() {
        return Result.success(adminBackupService.getSchedule());
    }

    @PutMapping("/schedule")
    public Result<Void> saveSchedule(@RequestBody BackupScheduleDTO dto) {
        try {
            adminBackupService.saveSchedule(dto);
            return Result.success("保存成功", null);
        } catch (Exception e) {
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    @PostMapping("/restore")
    public Result<BackupRecord> restore(@RequestParam("file") MultipartFile file) {
        try {
            return Result.success("恢复任务已提交（模拟）", adminBackupService.restore(file));
        } catch (Exception e) {
            return Result.error("恢复失败: " + e.getMessage());
        }
    }

    @GetMapping("/download/{id}")
    public void download(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        var file = adminBackupService.download(id);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + file.filename() + "\"");
        response.setContentType("application/octet-stream");
        try (var in = file.stream(); var out = response.getOutputStream()) {
            in.transferTo(out);
            out.flush();
        }
    }
}

