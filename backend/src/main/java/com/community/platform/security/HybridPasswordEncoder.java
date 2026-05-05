package com.community.platform.security;

import com.community.platform.util.MD5Util;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器（新写入使用 BCrypt，登录兼容历史 MD5）
 */
public class HybridPasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        if (isBcryptHash(encodedPassword)) {
            return bcrypt.matches(rawPassword, encodedPassword);
        }
        return MD5Util.verify(rawPassword.toString(), encodedPassword);
    }

    public static boolean isBcryptHash(String value) {
        if (value == null) return false;
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
