package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.platform.generated.entity.SupportLike;
import com.community.platform.generated.mapper.SupportLikeMapper;
import com.community.platform.service.SupportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 支持与点赞相关服务实现
 */
@Service
public class SupportServiceImpl implements SupportService {

    @Autowired
    private SupportLikeMapper supportLikeMapper;

    private SupportLike getOrCreate() {
        LambdaQueryWrapper<SupportLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.last("LIMIT 1");
        SupportLike like = supportLikeMapper.selectOne(wrapper);
        if (like == null) {
            like = new SupportLike();
            like.setTotalCount(0L);
            like.setCreatedAt(LocalDateTime.now());
            like.setUpdatedAt(LocalDateTime.now());
            supportLikeMapper.insert(like);
        }
        return like;
    }

    @Override
    public long getLikeCount() {
        return getOrCreate().getTotalCount();
    }

    @Override
    @Transactional
    public long addLike() {
        SupportLike like = getOrCreate();
        long newCount = (like.getTotalCount() == null ? 0L : like.getTotalCount()) + 1;
        like.setTotalCount(newCount);
        like.setUpdatedAt(LocalDateTime.now());
        supportLikeMapper.updateById(like);
        return newCount;
    }
}

