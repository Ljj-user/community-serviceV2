package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.common.Constants;
import com.community.platform.generated.entity.SysNotification;
import com.community.platform.generated.entity.SysUser;
import com.community.platform.generated.mapper.SysNotificationMapper;
import com.community.platform.generated.mapper.SysUserMapper;
import com.community.platform.service.AdminConfigService;
import com.community.platform.service.AlertExternalChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MVP 异常预警规则引擎
 */
@Service
public class AnomalyRuleEngineService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysNotificationMapper sysNotificationMapper;

    @Autowired
    private AdminConfigService adminConfigService;

    @Autowired
    private AlertExternalChannelService alertExternalChannelService;

    public void runAllRules() {
        Map<String, Object> config = adminConfigService.getAlert();
        boolean enableCare = asBool(config.get("enableCareInactivityAlert"), true);
        boolean enableSurge = asBool(config.get("enableDemandSurgeAlert"), true);
        if (enableCare) {
            runInactiveCareRule();
        }
        if (enableSurge) {
            runDemandSurgeRule();
        }
    }

    public void runInactiveCareRule() {
        Map<String, Object> config = adminConfigService.getAlert();
        int inactiveDays = asInt(config.get("careInactivityDays"), 3);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(Math.max(1, inactiveDays));
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();

        List<Map<String, Object>> candidates = jdbcTemplate.queryForList("""
                SELECT u.id AS user_id, u.community_id
                FROM sys_user u
                INNER JOIN care_subject_profile c ON c.user_id=u.id
                WHERE u.is_deleted=0
                  AND u.status=?
                  AND u.role=?
                  AND u.community_id IS NOT NULL
                  AND c.is_deleted=0
                  AND c.monitor_enabled=1
                  AND (u.last_login_at IS NULL OR u.last_login_at < ?)
                """, Constants.USER_STATUS_ENABLED, Constants.ROLE_NORMAL_USER, cutoff);
        for (Map<String, Object> row : candidates) {
            Long userId = asLong(row.get("user_id"), null);
            Long communityId = asLong(row.get("community_id"), null);
            if (communityId == null) continue;
            String dedupKey = "CARE_INACTIVE:" + userId + ":" + today;
            if (existsAlertToday(dedupKey, todayStart)) {
                continue;
            }
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null) continue;
            String triggerRule = "连续" + inactiveDays + "天未登录";
            String action = "建议社区管理员主动电话关怀或上门核实";
            createAlertEvent("CARE_INACTIVE", communityId, null, userId, 2, triggerRule, action,
                    "{\"days\":" + inactiveDays + "}", dedupKey);
            dispatchAlertNotifications(config, communityId,
                    "关怀预警：重点人群未登录",
                    "用户「" + safeName(user) + "」" + triggerRule + "，" + action,
                    Constants.NOTIF_REF_CARE_ALERT,
                    communityId,
                    todayStart);
        }
    }

    public void runDemandSurgeRule() {
        Map<String, Object> config = adminConfigService.getAlert();
        int minThreshold = asInt(config.get("surge24hMinRequests"), 5);
        double multiplier = asDouble(config.get("surgeMultiplier"), 2.0D);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from24h = now.minusHours(24);
        LocalDateTime baselineStart = now.minusDays(8);
        LocalDateTime baselineEnd = now.minusDays(1);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        List<Map<String, Object>> communities = jdbcTemplate.queryForList("""
                SELECT community_id, COUNT(1) AS cnt
                FROM service_request
                WHERE is_deleted=0
                  AND community_id IS NOT NULL
                  AND created_at >= ?
                GROUP BY community_id
                """, from24h);
        for (Map<String, Object> row : communities) {
            Long communityId = asLong(row.get("community_id"), null);
            long currentCnt = asLong(row.get("cnt"), 0L);
            if (communityId == null) continue;
            Double avg = jdbcTemplate.queryForObject("""
                    SELECT AVG(day_cnt) FROM (
                      SELECT DATE(created_at) dt, COUNT(1) AS day_cnt
                      FROM service_request
                      WHERE is_deleted=0
                        AND community_id=?
                        AND created_at BETWEEN ? AND ?
                      GROUP BY DATE(created_at)
                    ) t
                    """, Double.class, communityId, baselineStart, baselineEnd);
            double baseline = avg == null ? 0D : avg;
            double threshold = Math.max(minThreshold, baseline * multiplier);
            if (currentCnt < threshold || currentCnt <= 0) continue;

            String dedupKey = "DEMAND_SURGE:" + communityId + ":" + LocalDate.now();
            if (existsAlertToday(dedupKey, todayStart)) {
                continue;
            }
            String triggerRule = "24h需求量突增（当前" + currentCnt + "，基线" + String.format("%.1f", baseline) + "）";
            String action = "建议核查社区突发事件并增派志愿服务调度";
            String snapshot = "{\"current24h\":" + currentCnt + ",\"baseline7d\":" + String.format("%.2f", baseline)
                    + ",\"threshold\":" + String.format("%.2f", threshold) + "}";
            createAlertEvent("DEMAND_SURGE", communityId, null, null, 3, triggerRule, action, snapshot, dedupKey);
            dispatchAlertNotifications(config, communityId,
                    "风险预警：社区求助量骤增",
                    triggerRule + "，" + action,
                    Constants.NOTIF_REF_ANOMALY_ALERT,
                    communityId,
                    todayStart);
        }
    }

    private void createAlertEvent(String code, Long communityId, Long requestId, Long targetUserId, int severity,
                                  String triggerRule, String suggestionAction, String snapshot, String dedupKey) {
        jdbcTemplate.update("""
                INSERT INTO anomaly_alert_event(alert_code, community_id, request_id, target_user_id, severity,
                trigger_rule, suggestion_action, rule_snapshot, dedup_key, occurred_at, created_at)
                VALUES(?,?,?,?,?,?,?,?,?,NOW(),NOW())
                """, code, communityId, requestId, targetUserId, severity, triggerRule, suggestionAction, snapshot, dedupKey);
    }

    private boolean existsAlertToday(String dedupKey, LocalDateTime todayStart) {
        Integer c = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM anomaly_alert_event
                WHERE dedup_key=? AND occurred_at>=?
                """, Integer.class, dedupKey, todayStart);
        return c != null && c > 0;
    }

    private void notifyCommunityAdmins(Long communityId, String title, String summary,
                                       String refType, Long refId, LocalDateTime todayStart) {
        LambdaQueryWrapper<SysUser> aw = new LambdaQueryWrapper<>();
        aw.eq(SysUser::getIsDeleted, 0)
                .eq(SysUser::getStatus, Constants.USER_STATUS_ENABLED)
                .eq(SysUser::getRole, Constants.ROLE_COMMUNITY_ADMIN)
                .eq(SysUser::getCommunityId, communityId);
        List<SysUser> admins = sysUserMapper.selectList(aw);
        for (SysUser admin : admins) {
            LambdaQueryWrapper<SysNotification> nw = new LambdaQueryWrapper<>();
            nw.eq(SysNotification::getRecipientUserId, admin.getId())
                    .eq(SysNotification::getRefType, refType)
                    .eq(SysNotification::getRefId, refId)
                    .eq(SysNotification::getTitle, title)
                    .ge(SysNotification::getCreatedAt, todayStart)
                    .last("LIMIT 1");
            if (sysNotificationMapper.selectOne(nw) != null) {
                continue;
            }
            SysNotification n = new SysNotification();
            n.setRecipientUserId(admin.getId());
            n.setMsgCategory(Constants.NOTIFICATION_CATEGORY_BUSINESS);
            n.setReadStatus(Constants.NOTIFICATION_UNREAD);
            n.setRefType(refType);
            n.setRefId(refId);
            n.setTitle(title);
            n.setSummary(summary);
            n.setCreatedAt(LocalDateTime.now());
            sysNotificationMapper.insert(n);
        }
    }

    private void dispatchAlertNotifications(Map<String, Object> config, Long communityId, String title, String summary,
                                            String refType, Long refId, LocalDateTime todayStart) {
        List<String> channels = parseChannels(config.get("alertNotifyChannels"));
        if (channels.contains("IN_APP")) {
            notifyCommunityAdmins(communityId, title, summary, refType, refId, todayStart);
        }
        if (channels.contains("EMAIL")) {
            alertExternalChannelService.sendEmailAlert(communityId, title, summary);
        }
        if (channels.contains("SMS")) {
            alertExternalChannelService.sendSmsAlert(communityId, title, summary);
        }
    }

    private List<String> parseChannels(Object rawChannels) {
        if (rawChannels == null) {
            return List.of("IN_APP");
        }
        List<String> out = new ArrayList<>();
        if (rawChannels instanceof List<?> list) {
            for (Object c : list) {
                if (c == null) {
                    continue;
                }
                String item = String.valueOf(c).trim().toUpperCase();
                if (!item.isBlank()) {
                    out.add(item);
                }
            }
        } else {
            String text = String.valueOf(rawChannels);
            for (String s : text.split(",")) {
                String item = s == null ? "" : s.trim().toUpperCase();
                if (!item.isBlank()) {
                    out.add(item);
                }
            }
        }
        if (out.isEmpty()) {
            out.add("IN_APP");
        }
        return out;
    }

    private int asInt(Object raw, int def) {
        if (raw == null) return def;
        if (raw instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (Exception e) {
            return def;
        }
    }

    private long asLong(Object raw, Long def) {
        if (raw == null) return def == null ? 0L : def;
        if (raw instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (Exception e) {
            return def == null ? 0L : def;
        }
    }

    private double asDouble(Object raw, double def) {
        if (raw == null) return def;
        if (raw instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (Exception e) {
            return def;
        }
    }

    private boolean asBool(Object raw, boolean def) {
        if (raw == null) return def;
        if (raw instanceof Boolean b) return b;
        String s = String.valueOf(raw).trim();
        if ("1".equals(s) || "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s)) return true;
        if ("0".equals(s) || "false".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s)) return false;
        return def;
    }

    private String safeName(SysUser user) {
        if (user.getRealName() != null && !user.getRealName().isBlank()) return user.getRealName();
        if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername();
        return String.valueOf(user.getId());
    }
}
