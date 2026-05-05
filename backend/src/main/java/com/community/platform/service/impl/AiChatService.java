package com.community.platform.service.impl;

import com.community.platform.dto.AiChatResponseVO;
import com.community.platform.dto.AiAnalysisRecordVO;
import com.community.platform.dto.AiOrderDraftVO;
import com.community.platform.dto.AiChatRequest;
import com.community.platform.dto.PageResult;
import com.community.platform.generated.entity.SysConfig;
import com.community.platform.generated.mapper.SysConfigMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class AiChatService {

    private static final String SYS_KEY_AI = "ai";

    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${app.ai.base-url:https://api.deepseek.com}")
    private String aiBaseUrl;

    @Value("${app.ai.model:deepseek-v4-flash}")
    private String aiModel;

    @Value("${app.ai.api-key:}")
    private String aiApiKey;

    @Autowired(required = false)
    private SysConfigMapper sysConfigMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AiRuntimeConfig runtimeConfig() {
        AiRuntimeConfig c = new AiRuntimeConfig();
        c.enabled = aiEnabled;
        c.baseUrl = aiBaseUrl;
        c.model = aiModel;
        c.apiKey = resolvedApiKey();

        try {
            if (sysConfigMapper == null) return c;
            SysConfig row = sysConfigMapper.selectByConfigKey(SYS_KEY_AI);
            if (row == null || row.getConfigValue() == null) return c;
            var m = row.getConfigValue();
            if (m.containsKey("enabled")) c.enabled = toBool(m.get("enabled"), c.enabled);
            if (m.containsKey("baseUrl")) c.baseUrl = toStr(m.get("baseUrl"), c.baseUrl);
            if (m.containsKey("model")) c.model = toStr(m.get("model"), c.model);
            if (m.containsKey("apiKey")) {
                String k = toStr(m.get("apiKey"), "");
                if (!k.isBlank()) c.apiKey = k.trim();
            }
        } catch (Exception ignored) {
            // 任何异常都不影响核心业务：回退到 yml/env 默认值
        }
        return c;
    }

    private static boolean toBool(Object v, boolean def) {
        if (v == null) return def;
        if (v instanceof Boolean b) return b;
        String s = String.valueOf(v).trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return def;
        return s.equals("1") || s.equals("true") || s.equals("yes") || s.equals("y");
    }

    private static String toStr(Object v, String def) {
        if (v == null) return def;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? def : s;
    }

    private String resolvedApiKey() {
        if (aiApiKey != null && !aiApiKey.isBlank()) return aiApiKey.trim();
        String env = System.getenv("APP_AI_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        // 兼容常见命名（可选）
        env = System.getenv("DEEPSEEK_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        return "";
    }

    public AiChatResponseVO chat(Long userId, String message, List<AiChatRequest.HistoryMessage> history) {
        AiRuntimeConfig cfg = runtimeConfig();
        String text = message == null ? "" : message.trim();
        if (text.isEmpty()) {
            throw new RuntimeException("消息不能为空");
        }
        if (looksLikeDateTimeQuestion(text)) {
            return storeRecord(userId, text, buildDateTimeAnswer(), "mobile_assistant");
        }
        if (looksLikeModelQuestion(text)) {
            return storeRecord(userId, text, buildModelAnswer(cfg), "mobile_assistant");
        }
        if (looksLikeDemand(text)) {
            return storeRecord(userId, text, buildDemandDraft(text), "mobile_assistant");
        }
        if (cfg.enabled && cfg.apiKey != null && !cfg.apiKey.isBlank()) {
            AiChatResponseVO byModel = callModelForFaq(text, history);
            if (byModel != null) {
                return storeRecord(userId, text, byModel, "mobile_assistant");
            }
        }
        return storeRecord(userId, text, buildFaqFallback(text), "mobile_assistant");
    }

    private boolean looksLikeDemand(String text) {
        return text.contains("帮我")
                || text.contains("需求")
                || text.contains("找人")
                || text.contains("发布")
                || text.contains("买菜")
                || text.contains("陪诊")
                || text.contains("打扫");
    }

    private boolean looksLikeDateTimeQuestion(String text) {
        return text.contains("今天周几")
                || text.contains("星期几")
                || text.contains("今天几号")
                || text.contains("几月几号")
                || text.contains("现在几点")
                || (text.contains("日期") && text.contains("时间"));
    }

    private boolean looksLikeModelQuestion(String text) {
        return text.contains("什么模型")
                || text.contains("你是模型")
                || text.contains("模型是")
                || text.contains("哪个模型");
    }

    private AiChatResponseVO buildDateTimeAnswer() {
        LocalDateTime now = LocalDateTime.now();
        String weekday = now.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINA);
        String reply = String.format(
                Locale.ROOT,
                "现在是 %d年%d月%d日（%s）%02d:%02d。",
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                weekday,
                now.getHour(),
                now.getMinute()
        );
        AiChatResponseVO vo = new AiChatResponseVO();
        vo.setMode("FAQ");
        vo.setReply(reply);
        return vo;
    }

    private AiChatResponseVO buildModelAnswer(AiRuntimeConfig cfg) {
        String provider = cfg.baseUrl == null ? "当前配置" : cfg.baseUrl;
        String model = cfg.model == null ? "未配置" : cfg.model;
        String reply = String.format(Locale.ROOT, "当前接入模型：%s（%s）。", model, provider);
        AiChatResponseVO vo = new AiChatResponseVO();
        vo.setMode("FAQ");
        vo.setReply(reply);
        return vo;
    }

    private AiChatResponseVO buildDemandDraft(String text) {
        AiOrderDraftVO draft = new AiOrderDraftVO();
        draft.setServiceType(detectServiceType(text));
        draft.setUrgencyLevel(detectUrgency(text));
        draft.setExpectedTime(detectExpectedTime(text));
        draft.setTags(detectTags(text));
        draft.setDescription(buildDescription(text, draft));

        AiChatResponseVO vo = new AiChatResponseVO();
        vo.setMode("DEMAND_DRAFT");
        vo.setOrderDraft(draft);
        vo.setReply("我已根据你的描述生成需求草稿，你可以一键创建订单。");
        return vo;
    }

    private AiChatResponseVO storeRecord(Long userId, String inputText, AiChatResponseVO response, String scene) {
        if (userId == null || response == null) return response;
        try {
            Long communityId = jdbcTemplate.queryForObject(
                    "SELECT community_id FROM sys_user WHERE id=? LIMIT 1",
                    Long.class,
                    userId
            );
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("mode", response.getMode());
            result.put("reply", response.getReply());
            result.put("orderDraft", response.getOrderDraft());
            String resultJson = objectMapper.writeValueAsString(result);
            jdbcTemplate.update("""
                    INSERT INTO ai_analysis_record(user_id, community_id, scene, input_text, result_mode, result_json, applied_to_form, submitted_success, created_at, updated_at)
                    VALUES(?,?,?,?,?,?,0,0,NOW(3),NOW(3))
                    """, userId, communityId, scene, inputText, response.getMode(), resultJson);
            Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            response.setAnalysisRecordId(id);
        } catch (Exception ignored) {
            // AI 记录失败不能影响主链路
        }
        return response;
    }

    private String detectServiceType(String text) {
        // 必须命中后端发布白名单中的中文枚举，否则 AI 草稿无法一键带入发布页
        if (containsAny(text, "立即", "马上", "现在就要", "紧急", "摔倒", "晕倒", "受伤", "报警", "急救")) {
            return "应急帮助（紧急求助）";
        }
        if (containsAny(text, "陪诊", "陪护", "就医", "看病", "挂号", "取报告", "老人", "老年", "长者")) {
            return "助老服务（陪护 / 陪诊）";
        }
        if (containsAny(text, "买菜", "代买", "跑腿", "取药", "送药", "代办")) {
            return "代办服务（买菜 / 取药）";
        }
        if (containsAny(text, "打扫", "清洁", "保洁", "家政", "卫生")) {
            return "家政清洁";
        }
        if (containsAny(text, "聊天", "陪伴", "倾诉", "心理", "说说话", "解闷")) {
            return "心理陪伴 / 聊天";
        }
        if (containsAny(text, "活动", "签到", "布置", "搬运", "现场引导", "秩序维护")) {
            return "社区活动支持";
        }
        return "助老服务（陪护 / 陪诊）";
    }

    private int detectUrgency(String text) {
        if (text.contains("立刻") || text.contains("马上") || text.contains("非常紧急")) return 4;
        if (text.contains("尽快") || text.contains("紧急")) return 3;
        return 2;
    }

    private String detectExpectedTime(String text) {
        LocalDate targetDay = LocalDate.now();
        LocalTime targetTime = LocalTime.of(9, 0);
        if (text.contains("明天")) targetDay = targetDay.plusDays(1);
        if (text.contains("后天")) targetDay = targetDay.plusDays(2);
        if (text.contains("下午")) targetTime = LocalTime.of(15, 0);
        if (text.contains("晚上")) targetTime = LocalTime.of(19, 0);
        if (text.contains("中午")) targetTime = LocalTime.NOON;
        return LocalDateTime.of(targetDay, targetTime).toString();
    }

    private List<String> detectTags(String text) {
        List<String> tags = new ArrayList<>();
        if (containsAny(text, "老人", "老年", "长者")) tags.add("老人关怀");
        if (containsAny(text, "残疾", "行动不便", "轮椅")) tags.add("行动不便");
        if (containsAny(text, "独居", "一个人住")) tags.add("独居");
        if (containsAny(text, "陪诊", "就医", "看病", "挂号")) tags.add("就医陪诊");
        if (containsAny(text, "买菜", "取药", "跑腿", "代办")) tags.add("生活代办");
        if (tags.isEmpty()) tags.add("社区互助");
        return tags;
    }

    private String buildDescription(String raw, AiOrderDraftVO draft) {
        String cleaned = raw == null ? "" : raw
                .replace("AI生成：", "")
                .replace("ai生成：", "")
                .replace("AI生成", "")
                .trim();
        cleaned = cleaned.replaceAll("[；;。]+$", "");

        StringBuilder sb = new StringBuilder();
        sb.append("需求内容：").append(cleaned.isBlank() ? "需要志愿者协助处理线下服务" : cleaned).append("。");

        if (draft.getServiceType() != null && !draft.getServiceType().isBlank()) {
            sb.append("\n服务类型：").append(draft.getServiceType()).append("。");
        }
        if (draft.getTags() != null && !draft.getTags().isEmpty()) {
            sb.append("\n补充标签：").append(String.join("、", draft.getTags())).append("。");
        }
        sb.append("\n请志愿者提前电话联系，再确认上门或陪同时间。");
        return sb.toString();
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isBlank() || keywords == null) return false;
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private AiChatResponseVO buildFaqFallback(String text) {
        String answer;
        if (text.contains("怎么发布") || text.contains("发布需求")) {
            answer = "在“任务中心”点击“我要发布”，填写服务类型、地址和描述后提交即可。";
        } else if (text.contains("积分") || text.contains("时间币")) {
            answer = "完成服务并确认后会进行积分/时间币结算；居民发布需求时会做基础余额校验。";
        } else if (text.contains("规则") || text.contains("服务规则")) {
            answer = "需求流程为：发布->审核->认领->完成。系统会限制非法状态回退，并支持申诉与管理员干预。";
        } else {
            answer = "你可以问我：怎么发布需求、积分怎么算、服务规则是什么；也可以直接说需求，我会帮你生成订单草稿。";
        }
        AiChatResponseVO vo = new AiChatResponseVO();
        vo.setMode("FAQ");
        vo.setReply(answer);
        return vo;
    }

    private AiChatResponseVO callModelForFaq(String text, List<AiChatRequest.HistoryMessage> history) {
        try {
            AiRuntimeConfig cfg = runtimeConfig();
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content", """
                    你是社区公益服务平台问答助手。
                    要求：
                    1) 优先给出具体可执行步骤；
                    2) 语气自然，不要重复模板句；
                    3) 若用户问日期时间，可直接根据当前中国时间回答；
                    4) 优先返回 JSON：{"mode":"FAQ","reply":"..."}；如果未按 JSON 返回，也要保证内容可直接展示。
                    当前中国时间：%s
                    """.formatted(LocalDateTime.now())
            ));

            if (history != null && !history.isEmpty()) {
                int start = Math.max(0, history.size() - 8);
                for (int i = start; i < history.size(); i++) {
                    AiChatRequest.HistoryMessage h = history.get(i);
                    if (h == null) continue;
                    String role = normalizeRole(h.getRole());
                    String content = h.getText() == null ? "" : h.getText().trim();
                    if (content.isBlank()) continue;
                    messages.add(Map.of("role", role, "content", content));
                }
            }
            messages.add(Map.of("role", "user", "content", text));

            String body = objectMapper.writeValueAsString(Map.of(
                    "model", cfg.model,
                    "messages", messages,
                    "temperature", 0.35
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cfg.baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + cfg.apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) return null;
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) return null;

            String reply = parseReplyContent(content);
            AiChatResponseVO vo = new AiChatResponseVO();
            vo.setMode("FAQ");
            vo.setReply(reply);
            return vo;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String parseReplyContent(String content) {
        String c = content == null ? "" : content.trim();
        if (c.isBlank()) return "已收到你的问题。";
        try {
            JsonNode parsed = objectMapper.readTree(c);
            String r = parsed.path("reply").asText("");
            if (!r.isBlank()) return r.trim();
        } catch (Exception ignored) {
        }
        int start = c.indexOf('{');
        int end = c.lastIndexOf('}');
        if (start >= 0 && end > start) {
            try {
                JsonNode parsed = objectMapper.readTree(c.substring(start, end + 1));
                String r = parsed.path("reply").asText("");
                if (!r.isBlank()) return r.trim();
            } catch (Exception ignored) {
            }
        }
        return c;
    }

    private String normalizeRole(String role) {
        if (role == null) return "user";
        String r = role.trim().toLowerCase(Locale.ROOT);
        if ("assistant".equals(r) || "ai".equals(r)) return "assistant";
        return "user";
    }

    private static class AiRuntimeConfig {
        boolean enabled;
        String baseUrl;
        String model;
        String apiKey;
    }

    public PageResult<AiAnalysisRecordVO> listMine(Long userId, long page, long size) {
        long offset = Math.max(0, page - 1) * size;
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ai_analysis_record WHERE user_id=?",
                Integer.class,
                userId
        );
        List<AiAnalysisRecordVO> rows = jdbcTemplate.query("""
                SELECT a.id,
                       a.user_id AS userId,
                       u.username,
                       u.real_name AS realName,
                       a.community_id AS communityId,
                       r.name AS communityName,
                       a.scene,
                       a.input_text AS inputText,
                       a.result_mode AS resultMode,
                       CAST(a.result_json AS CHAR) AS resultJson,
                       a.applied_to_form AS appliedToForm,
                       a.submitted_success AS submittedSuccess,
                       DATE_FORMAT(a.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt
                FROM ai_analysis_record a
                LEFT JOIN sys_user u ON u.id=a.user_id
                LEFT JOIN sys_region r ON r.id=a.community_id
                WHERE a.user_id=?
                ORDER BY a.id DESC
                LIMIT ? OFFSET ?
                """, (rs, rowNum) -> {
            AiAnalysisRecordVO vo = new AiAnalysisRecordVO();
            vo.setId(rs.getLong("id"));
            vo.setUserId(rs.getLong("userId"));
            vo.setUsername(rs.getString("username"));
            vo.setRealName(rs.getString("realName"));
            vo.setCommunityId(toLong(rs.getObject("communityId")));
            vo.setCommunityName(rs.getString("communityName"));
            vo.setScene(rs.getString("scene"));
            vo.setInputText(rs.getString("inputText"));
            vo.setResultMode(rs.getString("resultMode"));
            vo.setResultJson(rs.getString("resultJson"));
            vo.setAppliedToForm(rs.getInt("appliedToForm"));
            vo.setSubmittedSuccess(rs.getInt("submittedSuccess"));
            vo.setCreatedAt(rs.getString("createdAt"));
            return vo;
        }, userId, size, offset);
        return PageResult.of(rows, total == null ? 0 : total, page, size);
    }

    public void markApplied(Long userId, Long id) {
        int affected = jdbcTemplate.update("""
                UPDATE ai_analysis_record
                SET applied_to_form=1, updated_at=NOW(3)
                WHERE id=? AND user_id=?
                """, id, userId);
        if (affected <= 0) throw new RuntimeException("AI 记录不存在或无权限");
    }

    public void markSubmitted(Long userId, Long id) {
        if (id == null) return;
        jdbcTemplate.update("""
                UPDATE ai_analysis_record
                SET applied_to_form=1, submitted_success=1, updated_at=NOW(3)
                WHERE id=? AND user_id=?
                """, id, userId);
    }

    public PageResult<AiAnalysisRecordVO> listAdmin(long page, long size) {
        long offset = Math.max(0, page - 1) * size;
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM ai_analysis_record", Integer.class);
        List<AiAnalysisRecordVO> rows = jdbcTemplate.query("""
                SELECT a.id,
                       a.user_id AS userId,
                       u.username,
                       u.real_name AS realName,
                       a.community_id AS communityId,
                       r.name AS communityName,
                       a.scene,
                       a.input_text AS inputText,
                       a.result_mode AS resultMode,
                       CAST(a.result_json AS CHAR) AS resultJson,
                       a.applied_to_form AS appliedToForm,
                       a.submitted_success AS submittedSuccess,
                       DATE_FORMAT(a.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt
                FROM ai_analysis_record a
                LEFT JOIN sys_user u ON u.id=a.user_id
                LEFT JOIN sys_region r ON r.id=a.community_id
                ORDER BY a.id DESC
                LIMIT ? OFFSET ?
                """, (rs, rowNum) -> {
            AiAnalysisRecordVO vo = new AiAnalysisRecordVO();
            vo.setId(rs.getLong("id"));
            vo.setUserId(rs.getLong("userId"));
            vo.setUsername(rs.getString("username"));
            vo.setRealName(rs.getString("realName"));
            vo.setCommunityId(toLong(rs.getObject("communityId")));
            vo.setCommunityName(rs.getString("communityName"));
            vo.setScene(rs.getString("scene"));
            vo.setInputText(rs.getString("inputText"));
            vo.setResultMode(rs.getString("resultMode"));
            vo.setResultJson(rs.getString("resultJson"));
            vo.setAppliedToForm(rs.getInt("appliedToForm"));
            vo.setSubmittedSuccess(rs.getInt("submittedSuccess"));
            vo.setCreatedAt(rs.getString("createdAt"));
            return vo;
        }, size, offset);
        return PageResult.of(rows, total == null ? 0 : total, page, size);
    }

    public AiAnalysisRecordVO detailAdmin(Long id) {
        List<AiAnalysisRecordVO> rows = jdbcTemplate.query("""
                SELECT a.id,
                       a.user_id AS userId,
                       u.username,
                       u.real_name AS realName,
                       a.community_id AS communityId,
                       r.name AS communityName,
                       a.scene,
                       a.input_text AS inputText,
                       a.result_mode AS resultMode,
                       CAST(a.result_json AS CHAR) AS resultJson,
                       a.applied_to_form AS appliedToForm,
                       a.submitted_success AS submittedSuccess,
                       DATE_FORMAT(a.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt
                FROM ai_analysis_record a
                LEFT JOIN sys_user u ON u.id=a.user_id
                LEFT JOIN sys_region r ON r.id=a.community_id
                WHERE a.id=?
                LIMIT 1
                """, (rs, rowNum) -> {
            AiAnalysisRecordVO vo = new AiAnalysisRecordVO();
            vo.setId(rs.getLong("id"));
            vo.setUserId(rs.getLong("userId"));
            vo.setUsername(rs.getString("username"));
            vo.setRealName(rs.getString("realName"));
            vo.setCommunityId(toLong(rs.getObject("communityId")));
            vo.setCommunityName(rs.getString("communityName"));
            vo.setScene(rs.getString("scene"));
            vo.setInputText(rs.getString("inputText"));
            vo.setResultMode(rs.getString("resultMode"));
            vo.setResultJson(rs.getString("resultJson"));
            vo.setAppliedToForm(rs.getInt("appliedToForm"));
            vo.setSubmittedSuccess(rs.getInt("submittedSuccess"));
            vo.setCreatedAt(rs.getString("createdAt"));
            return vo;
        }, id);
        if (rows.isEmpty()) throw new RuntimeException("AI 记录不存在");
        return rows.get(0);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return null;
        return Long.parseLong(text);
    }
}
