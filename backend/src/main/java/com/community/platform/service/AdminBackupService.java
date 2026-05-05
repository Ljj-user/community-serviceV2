package com.community.platform.service;

import com.community.platform.dto.BackupScheduleDTO;
import com.community.platform.dto.PageResult;
import com.community.platform.generated.entity.BackupRecord;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 超级管理员：数据导出与备份服务
 */
public interface AdminBackupService {

    BackupRecord runBackup();

    BackupRecord restore(MultipartFile file);

    PageResult<BackupRecord> history(int page, int size);

    void deleteRecord(Long id);

    BackupScheduleDTO getSchedule();

    void saveSchedule(BackupScheduleDTO dto);

    /**
     * 下载备份文件（返回输入流+文件名）
     */
    DownloadFile download(Long id);

    record DownloadFile(String filename, InputStream stream) {}
}

