package com.community.platform.service;

import com.community.platform.dto.UserProfileResponse;
import com.community.platform.dto.UserProfileUpdateRequest;

public interface UserProfileService {

    /**
     * 获取当前登录用户资料
     */
    UserProfileResponse getCurrentUserProfile();

    /**
     * 更新当前登录用户资料（含头像 URL）
     */
    UserProfileResponse updateCurrentUserProfile(UserProfileUpdateRequest request);

    /**
     * 仅更新头像 URL（供上传接口使用）
     */
    UserProfileResponse updateAvatar(String avatarUrl);
}

