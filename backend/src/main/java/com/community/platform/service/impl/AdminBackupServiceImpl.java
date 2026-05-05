package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.dto.BackupScheduleDTO;
import com.community.platform.dto.PageResult;
import com.community.platform.generated.entity.BackupRecord;
import com.community.platform.generated.entity.SysConfig;
import com.community.platform.generated.mapper.BackupRecordMapper;
import com.community.platform.generated.mapper.SysConfigMapper;
import com.community.platform.service.AdminBackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据导出与备份服务实现（毕设版：文件生成与恢复为轻量模拟，提供记录与下载能力）
 */
@Service
public class AdminBackupServiceImpl implements AdminBackupService {

    private static final String CONFIG_KEY_SCHEDULE = "backupSchedule";

    @Autowired
    private BackupRecordMapper backupRecordMapper;

    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.backup-dir:./uploads/backups}")
    private String backupDir;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BackupRecord runBackup() {
        ensureBackupDir();

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = "backup-" + ts + ".sql";
        Path filePath = Paths.get(backupDir, filename);

        String content = generateSqlSnapshot();
        writeTextFile(filePath, content);

        BackupRecord record = new BackupRecord();
        record.setRecordType("BACKUP");
        record.setFilename(filename);
        record.setFilePath(filePath.toAbsolutePath().toString());
        record.setStatus("SUCCESS");
        record.setNote("手动备份");
        record.setIsDeleted((byte) 0);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        BigDecimal sizeMb = sizeMb(filePath);
        record.setFileSizeMb(sizeMb);

        backupRecordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BackupRecord restore(MultipartFile file) {
        ensureBackupDir();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("备份文件不能为空");
        }

        String original = file.getOriginalFilename() == null ? "restore.sql" : file.getOriginalFilename();
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = "restore-" + ts + "-" + original.replaceAll("[\\\\/]", "_");
        Path filePath = Paths.get(backupDir, filename);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, filePath);
        } catch (IOException e) {
            throw new RuntimeException("保存恢复文件失败: " + e.getMessage(), e);
        }

        BackupRecord record = new BackupRecord();
        record.setRecordType("RESTORE");
        record.setFilename(filename);
        record.setFilePath(filePath.toAbsolutePath().toString());
        record.setStatus("SUCCESS");
        record.setNote("上传恢复（模拟）");
        record.setIsDeleted((byte) 0);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        record.setFileSizeMb(sizeMb(filePath));

        backupRecordMapper.insert(record);
        return record;
    }

    @Override
    public PageResult<BackupRecord> history(int page, int size) {
        int p = page < 1 ? 1 : page;
        int s = size < 1 ? 10 : size;
        LambdaQueryWrapper<BackupRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BackupRecord::getIsDeleted, 0).orderByDesc(BackupRecord::getCreatedAt);
        Page<BackupRecord> result = backupRecordMapper.selectPage(new Page<>(p, s), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void deleteRecord(Long id) {
        if (id == null) return;
        BackupRecord record = backupRecordMapper.selectById(id);
        if (record == null) return;
        record.setIsDeleted((byte) 1);
        record.setUpdatedAt(LocalDateTime.now());
        backupRecordMapper.updateById(record);
    }

    @Override
    public BackupScheduleDTO getSchedule() {
        SysConfig cfg = sysConfigMapper.selectByConfigKey(CONFIG_KEY_SCHEDULE);
        BackupScheduleDTO dto = new BackupScheduleDTO();
        if (cfg == null || cfg.getConfigValue() == null) {
            dto.setEnabled(false);
            dto.setCycle("daily");
            dto.setTime("02:00");
            dto.setKeepDays(30);
            return dto;
        }
        Map<String, Object> m = cfg.getConfigValue();
        dto.setEnabled(asBool(m.get("enabled"), false));
        dto.setCycle(asStr(m.get("cycle"), "daily"));
        dto.setTime(asStr(m.get("time"), "02:00"));
        dto.setKeepDays(asInt(m.get("keepDays"), 30));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSchedule(BackupScheduleDTO dto) {
        Map<String, Object> m = new HashMap<>();
        m.put("enabled", dto.getEnabled() != null && dto.getEnabled());
        m.put("cycle", dto.getCycle() == null ? "daily" : dto.getCycle());
        m.put("time", dto.getTime() == null ? "02:00" : dto.getTime());
        m.put("keepDays", dto.getKeepDays() == null ? 30 : dto.getKeepDays());

        SysConfig cfg = sysConfigMapper.selectByConfigKey(CONFIG_KEY_SCHEDULE);
        if (cfg == null) {
            cfg = new SysConfig();
            cfg.setConfigKey(CONFIG_KEY_SCHEDULE);
            cfg.setConfigValue(m);
            sysConfigMapper.insert(cfg);
        } else {
            cfg.setConfigValue(m);
            sysConfigMapper.updateById(cfg);
        }
    }

    @Override
    public DownloadFile download(Long id) {
        BackupRecord record = backupRecordMapper.selectById(id);
        if (record == null || record.getIsDeleted() != null && record.getIsDeleted() == 1) {
            throw new RuntimeException("记录不存在");
        }
        if (record.getFilePath() == null || record.getFilePath().isBlank()) {
            throw new RuntimeException("文件不存在");
        }
        Path path = Paths.get(record.getFilePath());
        if (!Files.exists(path)) {
            throw new RuntimeException("文件不存在");
        }
        try {
            InputStream in = Files.newInputStream(path);
            return new DownloadFile(record.getFilename() == null ? path.getFileName().toString() : record.getFilename(), in);
        } catch (IOException e) {
            throw new RuntimeException("下载失败: " + e.getMessage(), e);
        }
    }

    private void ensureBackupDir() {
        try {
            Files.createDirectories(Paths.get(backupDir));
        } catch (IOException e) {
            throw new RuntimeException("创建备份目录失败: " + e.getMessage(), e);
        }
    }

    private static void writeTextFile(Path path, String content) {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(path.toFile()), StandardCharsets.UTF_8)) {
            w.write(content);
        } catch (IOException e) {
            throw new RuntimeException("写入备份文件失败: " + e.getMessage(), e);
        }
    }

    private String generateSqlSnapshot() {
        StringBuilder sb = new StringBuilder(16 * 1024);
        sb.append("-- Community platform SQL snapshot").append(System.lineSeparator());
        sb.append("-- Generated at ").append(LocalDateTime.now()).append(System.lineSeparator());
        sb.append("SET NAMES utf8mb4;").append(System.lineSeparator());
        sb.append("SET FOREIGN_KEY_CHECKS = 0;").append(System.lineSeparator()).append(System.lineSeparator());

        List<String> tables = List.of(
                "sys_user",
                "service_request",
                "service_claim",
                "service_evaluation",
                "sys_notification",
                "sys_user_skill",
                "skill_tag_stat"
        );
        for (String table : tables) {
            appendTableData(sb, table);
        }

        sb.append("SET FOREIGN_KEY_CHECKS = 1;").append(System.lineSeparator());
        return sb.toString();
    }

    private void appendTableData(StringBuilder sb, String table) {
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList("SELECT * FROM " + table);
        } catch (Exception e) {
            sb.append("-- skip table ").append(table).append(": ").append(e.getMessage()).append(System.lineSeparator());
            return;
        }
        if (rows.isEmpty()) {
            sb.append("-- table ").append(table).append(" is empty").append(System.lineSeparator());
            return;
        }
        Map<String, Object> first = rows.get(0);
        List<String> columns = new ArrayList<>(first.keySet());
        sb.append("-- table ").append(table).append(" rows: ").append(rows.size()).append(System.lineSeparator());
        for (Map<String, Object> row : rows) {
            Map<String, Object> ordered = new LinkedHashMap<>();
            for (String c : columns) {
                ordered.put(c, row.get(c));
            }
            sb.append("INSERT INTO ").append(table).append(" (")
                    .append(String.join(", ", columns))
                    .append(") VALUES (")
                    .append(columns.stream().map(c -> toSqlValue(ordered.get(c))).reduce((a, b) -> a + ", " + b).orElse(""))
                    .append(");")
                    .append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());
    }

    private String toSqlValue(Object v) {
        if (v == null) return "NULL";
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        if (v instanceof Timestamp ts) return quote(ts.toLocalDateTime().toString().replace('T', ' '));
        if (v instanceof LocalDateTime dt) return quote(dt.toString().replace('T', ' '));
        if (v instanceof LocalDate d) return quote(d.toString());
        return quote(String.valueOf(v));
    }

    private String quote(String raw) {
        return "'" + raw.replace("\\", "\\\\").replace("'", "''") + "'";
    }

    private static BigDecimal sizeMb(Path path) {
        try {
            long bytes = Files.size(path);
            BigDecimal mb = BigDecimal.valueOf(bytes).divide(BigDecimal.valueOf(1024 * 1024L), 2, BigDecimal.ROUND_HALF_UP);
            return mb;
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean asBool(Object v, boolean def) {
        if (v == null) return def;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(v));
    }

    private static String asStr(Object v, String def) {
        if (v == null) return def;
        String s = String.valueOf(v);
        return s.isBlank() ? def : s;
    }

    private static int asInt(Object v, int def) {
        if (v == null) return def;
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }
}

