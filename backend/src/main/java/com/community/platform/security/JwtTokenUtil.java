package com.community.platform.security;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Token 工具类（简化版：使用内存存储）
 * 
 * 生产环境建议使用 JWT 或 Redis
 */
public class JwtTokenUtil {
    
    private static final ConcurrentHashMap<String, String> tokenStore = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    static {
        // 定期清理过期 token（24小时）
        scheduler.scheduleAtFixedRate(() -> {
            // 简化版：定期清理，实际应记录过期时间
        }, 1, 1, TimeUnit.HOURS);
    }
    
    /**
     * 生成 token
     */
    public static String generateToken(String username) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.put(token, username);
        return token;
    }
    
    /**
     * 验证 token
     */
    public static boolean validateToken(String token) {
        return token != null && tokenStore.containsKey(token);
    }
    
    /**
     * 从 token 获取用户名
     */
    public static String getUsernameFromToken(String token) {
        return tokenStore.get(token);
    }
    
    /**
     * 删除 token
     */
    public static void removeToken(String token) {
        tokenStore.remove(token);
    }

    /**
     * 用户修改登录名后，同步内存 token 映射，避免后续请求仍按旧用户名加载用户失败。
     */
    public static void updateUsernameForToken(String token, String newUsername) {
        if (token != null && newUsername != null && tokenStore.containsKey(token)) {
            tokenStore.put(token, newUsername);
        }
    }
}
