package com.community.platform.service;

/**
 * 支持与点赞相关服务
 */
public interface SupportService {

    /**
     * 获取全局点赞总数
     */
    long getLikeCount();

    /**
     * 点赞一次，返回最新总数
     */
    long addLike();
}

