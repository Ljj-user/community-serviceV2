package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.dto.AnnouncementVO;
import com.community.platform.dto.UserAnnouncementQueryDTO;
import com.community.platform.generated.entity.Announcement;
import com.community.platform.generated.entity.SysUser;
import com.community.platform.generated.mapper.AnnouncementMapper;
import com.community.platform.generated.mapper.SysUserMapper;
import com.community.platform.security.UserDetailsImpl;
import com.community.platform.service.UserAnnouncementService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 普通用户：只读公告（已发布、未删除）
 * 可见范围：全体（target_scope=0）；或推送社区与当前用户 community_id 一致（scope=1/2，楼栋推送需 target_building_id 为空才对该社区用户展示）
 */
@Service
public class UserAnnouncementServiceImpl implements UserAnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public IPage<AnnouncementVO> list(UserAnnouncementQueryDTO queryDTO) {
        Long communityId = resolveCurrentUserCommunityId();

        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getIsDeleted, 0);
        wrapper.eq(Announcement::getStatus, 1); // 已发布

        wrapper.and(w -> {
            w.eq(Announcement::getTargetScope, (byte) 0);
            if (communityId != null) {
                w.or(sub -> sub.eq(Announcement::getTargetScope, (byte) 1)
                        .eq(Announcement::getTargetCommunityId, communityId));
                w.or(sub -> sub.eq(Announcement::getTargetScope, (byte) 2)
                        .eq(Announcement::getTargetCommunityId, communityId)
                        .isNull(Announcement::getTargetBuildingId));
            }
        });

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.like(Announcement::getTitle, queryDTO.getKeyword().trim());
        }

        wrapper.orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getTopAt)
                .orderByDesc(Announcement::getPublishedAt)
                .orderByDesc(Announcement::getCreatedAt);

        Page<Announcement> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        IPage<Announcement> entityPage = announcementMapper.selectPage(page, wrapper);

        List<AnnouncementVO> vos = entityPage.getRecords().stream().map(this::toVO).toList();
        fillPublisher(vos);

        Page<AnnouncementVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public AnnouncementVO detail(Long id) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null || a.getIsDeleted() == 1 || a.getStatus() != 1) {
            throw new RuntimeException("公告不存在");
        }
        if (!canViewAnnouncement(a, resolveCurrentUserCommunityId())) {
            throw new RuntimeException("没有权限查看该公告");
        }
        AnnouncementVO vo = toVO(a);
        fillPublisher(List.of(vo));
        return vo;
    }

    private AnnouncementVO toVO(Announcement a) {
        AnnouncementVO vo = new AnnouncementVO();
        BeanUtils.copyProperties(a, vo);
        return vo;
    }

    private Long resolveCurrentUserCommunityId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            return null;
        }
        Long userId = userDetails.getUser().getId();
        if (userId == null) {
            return null;
        }
        SysUser fresh = sysUserMapper.selectById(userId);
        if (fresh == null || (fresh.getIsDeleted() != null && fresh.getIsDeleted() != 0)) {
            return null;
        }
        return fresh.getCommunityId();
    }

    private static boolean canViewAnnouncement(Announcement a, Long userCommunityId) {
        Byte scope = a.getTargetScope();
        if (scope == null || scope == 0) {
            return true;
        }
        if (userCommunityId == null) {
            return false;
        }
        if (!Objects.equals(a.getTargetCommunityId(), userCommunityId)) {
            return false;
        }
        if (scope == 1) {
            return true;
        }
        if (scope == 2) {
            return a.getTargetBuildingId() == null;
        }
        return false;
    }

    private void fillPublisher(List<AnnouncementVO> vos) {
        if (vos == null || vos.isEmpty()) return;
        List<Long> userIds = vos.stream()
                .map(AnnouncementVO::getPublisherUserId)
                .distinct()
                .toList();
        if (userIds.isEmpty()) return;
        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        Map<Long, SysUser> map = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        for (AnnouncementVO vo : vos) {
            SysUser u = map.get(vo.getPublisherUserId());
            if (u != null) vo.setPublisherName(u.getRealName());
        }
    }
}

