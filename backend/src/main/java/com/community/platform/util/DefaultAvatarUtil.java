package com.community.platform.util;

/**
 * 默认头像路径（与 {@code WebMvcConfig} 中 /static/avatars/** 映射到 uploads/avatars 一致）
 */
public final class DefaultAvatarUtil {

    public static final String MALE_RELATIVE = "/static/avatars/boy-blue.png";
    public static final String FEMALE_RELATIVE = "/static/avatars/gril-pink.png";

    private DefaultAvatarUtil() {
    }

    /**
     * 性别：0 未知 1 男 2 女；未知时默认使用男性头像
     */
    public static String resolve(Byte gender) {
        if (gender != null && gender == 2) {
            return FEMALE_RELATIVE;
        }
        return MALE_RELATIVE;
    }

    public static boolean isBundledDefaultPath(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return true;
        }
        String u = avatarUrl.trim();
        return u.endsWith("boy-blue.png") || u.endsWith("gril-pink.png");
    }
}
