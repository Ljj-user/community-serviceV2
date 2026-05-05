package com.community.platform.util;

import com.community.platform.common.Constants;

/**
 * 普通用户身份归一化：仅支持居民(1)、志愿者(2)，不再支持「双重身份」等历史取值。
 */
public final class IdentityTypeUtil {

    private IdentityTypeUtil() {
    }

    /**
     * 归一化身份：仅 1、2 有效；null 与其它值（含历史值 3）按居民处理。
     */
    public static Byte normalize(Byte identityType) {
        if (identityType == null) {
            return Constants.IDENTITY_RESIDENT;
        }
        if (identityType.equals(Constants.IDENTITY_RESIDENT)
                || identityType.equals(Constants.IDENTITY_VOLUNTEER)) {
            return identityType;
        }
        return Constants.IDENTITY_RESIDENT;
    }
}
