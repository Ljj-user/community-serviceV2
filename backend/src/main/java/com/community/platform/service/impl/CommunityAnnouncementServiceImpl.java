package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.Constants;
import com.community.platform.dto.AnnouncementQueryDTO;
import com.community.platform.dto.AnnouncementSaveDTO;
import com.community.platform.dto.AnnouncementVO;
import com.community.platform.generated.entity.Announcement;
import com.community.platform.generated.entity.SysUser;
import com.community.platform.generated.mapper.AnnouncementMapper;
import com.community.platform.generated.entity.SysRegion;
import com.community.platform.generated.mapper.SysUserMapper;
import com.community.platform.generated.mapper.SysRegionMapper;
import com.community.platform.service.CommunityAnnouncementService;
import com.community.platform.service.UserNotificationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommunityAnnouncementServiceImpl implements CommunityAnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysRegionMapper sysRegionMapper;

    @Autowired
    private UserNotificationService userNotificationService;

    @Override
    public IPage<AnnouncementVO> list(Long operatorUserId, AnnouncementQueryDTO queryDTO) {
        SysUser operator = getOperator(operatorUserId);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getIsDeleted, 0);

        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.like(Announcement::getTitle, queryDTO.getKeyword().trim());
        }
        if (queryDTO.getTargetScope() != null) {
            wrapper.eq(Announcement::getTargetScope, queryDTO.getTargetScope());
        }
        if (queryDTO.getTargetCommunityId() != null) {
            wrapper.eq(Announcement::getTargetCommunityId, queryDTO.getTargetCommunityId());
        }
        if (queryDTO.getTargetBuildingId() != null) {
            wrapper.eq(Announcement::getTargetBuildingId, queryDTO.getTargetBuildingId());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Announcement::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getIsTop() != null) {
            wrapper.eq(Announcement::getIsTop, queryDTO.getIsTop());
        }
        // 社区管理员只能看本社区公告
        if (isCommunityAdmin(operator)) {
            if (operator.getCommunityId() == null) {
                throw new RuntimeException("管理员未绑定社区，无法查看公告");
            }
            wrapper.eq(Announcement::getTargetCommunityId, operator.getCommunityId());
        }

        wrapper.orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getTopAt)
                .orderByDesc(Announcement::getPublishedAt)
                .orderByDesc(Announcement::getCreatedAt);

        Page<Announcement> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        IPage<Announcement> entityPage = announcementMapper.selectPage(page, wrapper);

        List<AnnouncementVO> records = entityPage.getRecords().stream().map(this::toVO).toList();
        fillPublisher(records);
        fillCommunity(records);

        Page<AnnouncementVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    @Override
    public AnnouncementVO detail(Long operatorUserId, Long id) {
        SysUser operator = getOperator(operatorUserId);
        Announcement a = announcementMapper.selectById(id);
        if (a == null || a.getIsDeleted() == 1) {
            throw new RuntimeException("公告不存在");
        }
        assertScope(operator, a.getTargetCommunityId());
        AnnouncementVO vo = toVO(a);
        fillPublisher(List.of(vo));
        fillCommunity(List.of(vo));
        return vo;
    }

    @Override
    @Transactional
    public Long save(Long publisherUserId, AnnouncementSaveDTO dto) {
        SysUser user = sysUserMapper.selectById(publisherUserId);
        if (user == null || user.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getRole() != Constants.ROLE_COMMUNITY_ADMIN && user.getRole() != Constants.ROLE_SUPER_ADMIN) {
            throw new RuntimeException("只有社区管理员可以发布公告");
        }
        if (isCommunityAdmin(user)) {
            if (user.getCommunityId() == null) {
                throw new RuntimeException("管理员未绑定社区，无法发布公告");
            }
            if (dto.getTargetScope() != null && dto.getTargetScope() != 0
                    && dto.getTargetCommunityId() != null
                    && !user.getCommunityId().equals(dto.getTargetCommunityId())) {
                throw new RuntimeException("社区管理员仅可发布本社区公告");
            }
            if (dto.getTargetScope() == null || dto.getTargetScope() != 0) {
                dto.setTargetCommunityId(user.getCommunityId());
            }
        }

        // scope校验：楼栋必须带communityId + buildingId；社区必须带communityId
        if (dto.getTargetScope() == 1 && dto.getTargetCommunityId() == null) {
            throw new RuntimeException("请选择推送社区");
        }
        if (dto.getTargetScope() == 2) {
            if (dto.getTargetCommunityId() == null) {
                throw new RuntimeException("请选择推送社区");
            }
            if (dto.getTargetBuildingId() == null) {
                throw new RuntimeException("请选择推送楼栋");
            }
        }

        Announcement a;
        boolean isNew = (dto.getId() == null);
        Byte previousStatus = null;
        if (isNew) {
            a = new Announcement();
            a.setCreatedAt(LocalDateTime.now());
            a.setIsDeleted((byte) 0);
            a.setPublisherUserId(publisherUserId);
        } else {
            a = announcementMapper.selectById(dto.getId());
            if (a == null || a.getIsDeleted() == 1) {
                throw new RuntimeException("公告不存在");
            }
            assertScope(user, a.getTargetCommunityId());
            previousStatus = a.getStatus();
        }

        a.setTitle(dto.getTitle());
        a.setContentHtml(dto.getContentHtml());
        a.setContentText(toPlainText(dto.getContentHtml()));
        a.setTargetScope(dto.getTargetScope());
        a.setTargetCommunityId(dto.getTargetCommunityId());
        a.setTargetBuildingId(dto.getTargetBuildingId());

        Byte status = dto.getStatus() != null ? dto.getStatus() : (byte) 1;
        a.setStatus(status);

        Byte isTop = dto.getIsTop() != null ? dto.getIsTop() : (byte) 0;
        a.setIsTop(isTop);
        if (isTop == 1) {
            a.setTopAt(a.getTopAt() != null ? a.getTopAt() : LocalDateTime.now());
        } else {
            a.setTopAt(null);
        }

        if (status == 1 && a.getPublishedAt() == null) {
            a.setPublishedAt(LocalDateTime.now());
        }

        a.setUpdatedAt(LocalDateTime.now());

        if (isNew) {
            announcementMapper.insert(a);
        } else {
            announcementMapper.updateById(a);
        }

        boolean turningPublished = (a.getStatus() != null && a.getStatus() == 1)
                && (previousStatus == null || previousStatus != 1);
        if (turningPublished) {
            userNotificationService.broadcastAnnouncement(a);
        }
        return a.getId();
    }

    @Override
    @Transactional
    public void delete(Long id, Long operatorUserId) {
        SysUser operator = getOperator(operatorUserId);
        Announcement a = announcementMapper.selectById(id);
        if (a == null || a.getIsDeleted() == 1) {
            return;
        }
        assertScope(operator, a.getTargetCommunityId());
        a.setIsDeleted((byte) 1);
        a.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(a);
    }

    @Override
    @Transactional
    public void setTop(Long id, boolean isTop, Long operatorUserId) {
        SysUser operator = getOperator(operatorUserId);
        Announcement a = announcementMapper.selectById(id);
        if (a == null || a.getIsDeleted() == 1) {
            throw new RuntimeException("公告不存在");
        }
        assertScope(operator, a.getTargetCommunityId());
        a.setIsTop((byte) (isTop ? 1 : 0));
        a.setTopAt(isTop ? LocalDateTime.now() : null);
        a.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(a);
    }

    private SysUser getOperator(Long operatorUserId) {
        SysUser operator = sysUserMapper.selectById(operatorUserId);
        if (operator == null || operator.getIsDeleted() == 1) {
            throw new RuntimeException("当前用户不存在");
        }
        return operator;
    }

    private boolean isCommunityAdmin(SysUser user) {
        return user != null && user.getRole() != null && user.getRole().equals(Constants.ROLE_COMMUNITY_ADMIN);
    }

    private boolean isSuperAdmin(SysUser user) {
        return user != null && user.getRole() != null && user.getRole().equals(Constants.ROLE_SUPER_ADMIN);
    }

    private void assertScope(SysUser operator, Long targetCommunityId) {
        if (isSuperAdmin(operator)) {
            return;
        }
        if (!isCommunityAdmin(operator)) {
            throw new RuntimeException("无权限操作公告");
        }
        if (operator.getCommunityId() == null) {
            throw new RuntimeException("管理员未绑定社区，无法操作公告");
        }
        if (targetCommunityId == null || !operator.getCommunityId().equals(targetCommunityId)) {
            throw new RuntimeException("仅可操作本社区公告");
        }
    }

    private AnnouncementVO toVO(Announcement a) {
        AnnouncementVO vo = new AnnouncementVO();
        BeanUtils.copyProperties(a, vo);
        return vo;
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

    private void fillCommunity(List<AnnouncementVO> vos) {
        if (vos == null || vos.isEmpty()) return;
        List<Long> ids = vos.stream()
                .map(AnnouncementVO::getTargetCommunityId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) return;
        Map<Long, SysRegion> map = sysRegionMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysRegion::getId, r -> r, (a, b) -> a));
        for (AnnouncementVO vo : vos) {
            if (vo.getTargetCommunityId() == null) continue;
            SysRegion r = map.get(vo.getTargetCommunityId());
            if (r != null) vo.setTargetCommunityName(r.getName());
        }
    }

    /**
     * 简单提取纯文本（用于搜索/摘要），不追求完美HTML解析
     */
    private String toPlainText(String html) {
        if (!StringUtils.hasText(html)) return null;
        return html.replaceAll("<[^>]+>", "").replace("&nbsp;", " ").trim();
    }
}

