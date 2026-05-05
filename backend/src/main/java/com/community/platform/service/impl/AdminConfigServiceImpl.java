package com.community.platform.service.impl;

import com.community.platform.generated.entity.SysConfig;
import com.community.platform.generated.mapper.SysConfigMapper;
import com.community.platform.service.AdminConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Map;
import java.util.List;

/**
 * 系统配置服务实现
 */
@Service
public class AdminConfigServiceImpl implements AdminConfigService {

    @Autowired
    private SysConfigMapper sysConfigMapper;

    private static final String KEY_BASIC = "basic";
    private static final String KEY_NOTICE = "notice";
    private static final String KEY_ALERT = "alert";
    private static final String KEY_AI = "ai";
    private static final String KEY_RUNTIME = "runtime";
    private static final String AI_FIELD_API_KEY = "apiKey";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient AI_HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public Map<String, Object> getBasic() {
        return getConfig(KEY_BASIC);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBasic(Map<String, Object> data) {
        saveConfig(KEY_BASIC, data);
    }

    @Override
    public Map<String, Object> getNotice() {
        return getConfig(KEY_NOTICE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveNotice(Map<String, Object> data) {
        saveConfig(KEY_NOTICE, data);
    }

    @Override
    public Map<String, Object> getAlert() {
        return getConfig(KEY_ALERT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAlert(Map<String, Object> data) {
        saveConfig(KEY_ALERT, data);
    }

    @Override
    public Map<String, Object> getAi() {
        Map<String, Object> raw = getConfig(KEY_AI);
        if (raw.isEmpty()) return Collections.emptyMap();

        Map<String, Object> safe = new LinkedHashMap<>(raw);
        Object apiKey = safe.remove(AI_FIELD_API_KEY);
        safe.put("hasApiKey", apiKey != null && !String.valueOf(apiKey).isBlank());
        return safe;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAi(Map<String, Object> data) {
        // 约定：
        // - enabled/baseUrl/model：覆盖保存
        // - apiKey：若缺失则保留原值；若显式传空字符串则清空
        Map<String, Object> current = getConfig(KEY_AI);
        Map<String, Object> next = new LinkedHashMap<>();

        // copy known fields first (保持顺序稳定)
        if (data != null) {
            if (data.containsKey("enabled")) next.put("enabled", data.get("enabled"));
            if (data.containsKey("baseUrl")) next.put("baseUrl", data.get("baseUrl"));
            if (data.containsKey("model")) next.put("model", data.get("model"));
        }

        boolean containsApiKey = data != null && data.containsKey(AI_FIELD_API_KEY);
        boolean clearApiKey = false;
        if (containsApiKey) {
            Object k = data.get(AI_FIELD_API_KEY);
            String s = k == null ? "" : String.valueOf(k);
            if (!s.isBlank()) {
                next.put(AI_FIELD_API_KEY, s.trim());
            } else {
                // 显式清空
                next.remove(AI_FIELD_API_KEY);
                clearApiKey = true;
            }
        } else if (current.containsKey(AI_FIELD_API_KEY)) {
            next.put(AI_FIELD_API_KEY, Objects.toString(current.get(AI_FIELD_API_KEY), ""));
        }

        // merge other unknown keys (forward compatibility)
        if (current != null) {
            for (Map.Entry<String, Object> e : current.entrySet()) {
                if (clearApiKey && AI_FIELD_API_KEY.equals(e.getKey())) {
                    continue;
                }
                if (!next.containsKey(e.getKey()) && e.getValue() != null) {
                    next.put(e.getKey(), e.getValue());
                }
            }
        }

        saveConfig(KEY_AI, next);
    }

    @Override
    public Map<String, Object> testAi(Map<String, Object> data) {
        Map<String, Object> current = getConfig(KEY_AI);
        String baseUrl = pickString(data, "baseUrl", pickString(current, "baseUrl", "https://api.deepseek.com"));
        String model = pickString(data, "model", pickString(current, "model", "deepseek-v4-flash"));

        // apiKey：优先本次提交，其次使用已存的 key；都没有则走环境变量
        String apiKey = pickString(data, AI_FIELD_API_KEY, "");
        if (apiKey.isBlank()) apiKey = pickString(current, AI_FIELD_API_KEY, "");
        if (apiKey.isBlank()) apiKey = pickEnv("APP_AI_API_KEY");
        if (apiKey.isBlank()) apiKey = pickEnv("DEEPSEEK_API_KEY");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("baseUrl", baseUrl);
        out.put("model", model);

        if (apiKey.isBlank()) {
            out.put("ok", false);
            out.put("error", "未配置 API Key（请在 AI 配置中填写，或设置环境变量 APP_AI_API_KEY）");
            return out;
        }

        long start = System.nanoTime();
        try {
            String body = OBJECT_MAPPER.writeValueAsString(Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a helpful assistant."),
                            Map.of("role", "user", "content", "ping")
                    ),
                    "stream", false,
                    "temperature", 0
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = AI_HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            long costMs = (System.nanoTime() - start) / 1_000_000;
            out.put("latencyMs", costMs);
            out.put("status", resp.statusCode());

            if (resp.statusCode() / 100 != 2) {
                out.put("ok", false);
                out.put("error", "模型请求失败");
                out.put("bodyPreview", preview(resp.body(), 400));
                return out;
            }

            JsonNode root = OBJECT_MAPPER.readTree(resp.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            out.put("ok", true);
            out.put("replyPreview", preview(content, 200));
            return out;
        } catch (Exception e) {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            out.put("latencyMs", costMs);
            out.put("ok", false);
            out.put("error", e.getMessage() == null ? "未知错误" : e.getMessage());
            return out;
        }
    }

    @Override
    public Map<String, Object> getRuntime() {
        return getConfig(KEY_RUNTIME);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRuntime(Map<String, Object> data) {
        saveConfig(KEY_RUNTIME, data);
    }

    private static String pickString(Map<String, Object> data, String key, String def) {
        if (data == null) return def;
        if (!data.containsKey(key)) return def;
        Object v = data.get(key);
        if (v == null) return def;
        String s = String.valueOf(v).trim();
        return s.isBlank() ? def : s;
    }

    private static String pickEnv(String key) {
        String v = System.getenv(key);
        return v == null ? "" : v.trim();
    }

    private static String preview(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() <= max) return t;
        return t.substring(0, max) + "…";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getConfig(String key) {
        SysConfig row = sysConfigMapper.selectByConfigKey(key);
        if (row == null || row.getConfigValue() == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> val = row.getConfigValue();
        return val == null ? Collections.emptyMap() : new LinkedHashMap<>(val);
    }

    private void saveConfig(String key, Map<String, Object> data) {
        SysConfig row = sysConfigMapper.selectByConfigKey(key);
        if (row == null) {
            row = new SysConfig();
            row.setConfigKey(key);
            row.setConfigValue(data);
            sysConfigMapper.insert(row);
        } else {
            row.setConfigValue(data);
            sysConfigMapper.updateById(row);
        }
    }
}
