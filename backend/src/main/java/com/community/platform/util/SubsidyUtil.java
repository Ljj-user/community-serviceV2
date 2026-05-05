package com.community.platform.util;

/**
 * 补贴人群判断（基于 sys_user.identity_tag）
 */
public final class SubsidyUtil {

    private SubsidyUtil() {
    }

    /**
     * 业务约定：identity_tag 可能是单值或包含多个标签的文本（逗号/空格等）。
     * 只要包含任意补贴关键词，即视为系统补贴人群。
     */
    public static boolean isSubsidized(String identityTag) {
        if (identityTag == null || identityTag.isBlank()) {
            return false;
        }
        String s = identityTag.trim();
        return s.contains("孤寡老人") || s.contains("残疾") || s.contains("低保");
    }
}

