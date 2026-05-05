package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.common.Constants;
import com.community.platform.dto.MobileAlertCardVO;
import com.community.platform.dto.NotificationUnreadCountVO;
import com.community.platform.dto.UserNotificationVO;
import com.community.platform.generated.entity.Announcement;
import com.community.platform.generated.entity.ServiceClaim;
import com.community.platform.generated.entity.ServiceRequest;
import com.community.platform.generated.entity.SysNotification;
import com.community.platform.generated.entity.SysUser;
import com.community.platform.generated.mapper.SysNotificationMapper;
import com.community.platform.generated.mapper.SysUserMapper;
import com.community.platform.service.UserNotificationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserNotificationServiceImpl implements UserNotificationService {

    @Autowired
    private SysNotificationMapper sysNotificationMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    private static String trimSummary(String text, int maxLen) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String t = text.trim();
        if (t.length() <= maxLen) {
            return t;
        }
        return t.substring(0, maxLen) + "…";
    }

    private void insert(Long recipientId, Byte category, String title, String summary,
                        String refType, Long refId) {
        SysNotification n = new SysNotification();
        n.setRecipientUserId(recipientId);
        n.setMsgCategory(category);
        n.setTitle(title);
        n.setSummary(summary);
        n.setReadStatus(Constants.NOTIFICATION_UNREAD);
        n.setRefType(refType);
        n.setRefId(refId);
        n.setCreatedAt(LocalDateTime.now());
        sysNotificationMapper.insert(n);
    }

    @Override
    public void notifyRequestAudited(ServiceRequest request, boolean approved, String rejectReason) {
        if (request == null || request.getRequesterUserId() == null) {
            return;
        }
        Long uid = request.getRequesterUserId();
        String typeLabel = StringUtils.hasText(request.getServiceType()) ? request.getServiceType() : "服务需求";
        if (approved) {
            insert(uid, Constants.NOTIFICATION_CATEGORY_BUSINESS,
                    "需求审核已通过",
                    trimSummary("您发布的「" + typeLabel + "」已通过审核，志愿者可在广场认领。", 500),
                    Constants.NOTIF_REF_REQUEST_AUDIT, request.getId());
        } else {
            String reason = StringUtils.hasText(rejectReason) ? rejectReason : "无";
            insert(uid, Constants.NOTIFICATION_CATEGORY_BUSINESS,
                    "需求审核未通过",
                    trimSummary("您发布的「" + typeLabel + "」未通过审核。原因：" + reason, 500),
                    Constants.NOTIF_REF_REQUEST_AUDIT, request.getId());
        }
    }

    @Override
    public void notifyServiceClaimed(ServiceRequest request, ServiceClaim claim) {
        if (request == null || request.getRequesterUserId() == null || claim == null) {
            return;
        }
        SysUser vol = sysUserMapper.selectById(claim.getVolunteerUserId());
        String volName = vol != null && StringUtils.hasText(vol.getRealName()) ? vol.getRealName() : "志愿者";
        String typeLabel = StringUtils.hasText(request.getServiceType()) ? request.getServiceType() : "服务需求";
        insert(request.getRequesterUserId(), Constants.NOTIFICATION_CATEGORY_BUSINESS,
                "志愿者已认领您的需求",
                trimSummary(volName + " 已认领您的「" + typeLabel + "」，请保持电话畅通。", 500),
                Constants.NOTIF_REF_SERVICE_CLAIM, request.getId());
    }

    @Override
    @Transactional
    public void broadcastAnnouncement(Announcement announcement) {
        if (announcement == null || announcement.getId() == null) {
            return;
        }
        String sum = trimSummary(StringUtils.hasText(announcement.getContentText())
                ? announcement.getContentText() : announcement.getTitle(), 400);
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.eq(SysUser::getIsDeleted, 0).eq(SysUser::getStatus, Constants.USER_STATUS_ENABLED);
        w.eq(SysUser::getRole, Constants.ROLE_NORMAL_USER);
        List<SysUser> users = sysUserMapper.selectList(w);
        String title = StringUtils.hasText(announcement.getTitle()) ? announcement.getTitle() : "社区公告";
        for (SysUser u : users) {
            insert(u.getId(), Constants.NOTIFICATION_CATEGORY_ANNOUNCEMENT,
                    "【公告】" + title, sum, Constants.NOTIF_REF_ANNOUNCEMENT, announcement.getId());
        }
    }

    @Override
    public NotificationUnreadCountVO countUnread(Long userId) {
        NotificationUnreadCountVO vo = new NotificationUnreadCountVO();
        LambdaQueryWrapper<SysNotification> wb = new LambdaQueryWrapper<>();
        wb.eq(SysNotification::getRecipientUserId, userId)
                .eq(SysNotification::getReadStatus, Constants.NOTIFICATION_UNREAD)
                .eq(SysNotification::getMsgCategory, Constants.NOTIFICATION_CATEGORY_BUSINESS);
        vo.setBusiness(sysNotificationMapper.selectCount(wb));

        LambdaQueryWrapper<SysNotification> wa = new LambdaQueryWrapper<>();
        wa.eq(SysNotification::getRecipientUserId, userId)
                .eq(SysNotification::getReadStatus, Constants.NOTIFICATION_UNREAD)
                .eq(SysNotification::getMsgCategory, Constants.NOTIFICATION_CATEGORY_ANNOUNCEMENT);
        vo.setAnnouncement(sysNotificationMapper.selectCount(wa));

        vo.setTotal(vo.getBusiness() + vo.getAnnouncement());
        return vo;
    }

    @Override
    public IPage<UserNotificationVO> pageMine(Long userId, Byte category, long page, long size) {
        LambdaQueryWrapper<SysNotification> w = new LambdaQueryWrapper<>();
        w.eq(SysNotification::getRecipientUserId, userId);
        if (category != null) {
            w.eq(SysNotification::getMsgCategory, category);
        }
        w.orderByDesc(SysNotification::getCreatedAt);
        Page<SysNotification> p = new Page<>(page, size);
        IPage<SysNotification> entityPage = sysNotificationMapper.selectPage(p, w);
        Page<UserNotificationVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public IPage<MobileAlertCardVO> pageMyAlertNotifications(Long userId, long page, long size, Boolean unreadOnly) {
        LambdaQueryWrapper<SysNotification> w = new LambdaQueryWrapper<>();
        w.eq(SysNotification::getRecipientUserId, userId)
                .eq(SysNotification::getMsgCategory, Constants.NOTIFICATION_CATEGORY_BUSINESS)
                .in(SysNotification::getRefType, Constants.NOTIF_REF_CARE_ALERT, Constants.NOTIF_REF_ANOMALY_ALERT);
        if (Boolean.TRUE.equals(unreadOnly)) {
            w.eq(SysNotification::getReadStatus, Constants.NOTIFICATION_UNREAD);
        }
        w.orderByDesc(SysNotification::getCreatedAt);
        Page<SysNotification> p = new Page<>(page, size);
        IPage<SysNotification> entityPage = sysNotificationMapper.selectPage(p, w);
        Page<MobileAlertCardVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toMobileAlertCard).toList());
        return voPage;
    }

    @Override
    public long countMyAlertUnread(Long userId) {
        LambdaQueryWrapper<SysNotification> w = new LambdaQueryWrapper<>();
        w.eq(SysNotification::getRecipientUserId, userId)
                .eq(SysNotification::getMsgCategory, Constants.NOTIFICATION_CATEGORY_BUSINESS)
                .eq(SysNotification::getReadStatus, Constants.NOTIFICATION_UNREAD)
                .in(SysNotification::getRefType, Constants.NOTIF_REF_CARE_ALERT, Constants.NOTIF_REF_ANOMALY_ALERT);
        return sysNotificationMapper.selectCount(w);
    }

    private UserNotificationVO toVO(SysNotification n) {
        UserNotificationVO vo = new UserNotificationVO();
        BeanUtils.copyProperties(n, vo);
        return vo;
    }

    private MobileAlertCardVO toMobileAlertCard(SysNotification n) {
        MobileAlertCardVO vo = new MobileAlertCardVO();
        vo.setId(n.getId());
        vo.setTitle(n.getTitle());
        vo.setSummary(n.getSummary());
        vo.setReadStatus(n.getReadStatus());
        vo.setCreatedAt(n.getCreatedAt());
        vo.setRefType(n.getRefType());
        vo.setRefId(n.getRefId());
        vo.setActionType("OPEN_ALERT_DETAIL");
        vo.setActionTarget("/mobile/admin/alerts/" + n.getRefId());

        if (Constants.NOTIF_REF_CARE_ALERT.equals(n.getRefType())) {
            vo.setAlertType("CARE_INACTIVE");
            vo.setAlertTypeLabel("关怀预警");
            vo.setSeverityLevel(2);
            vo.setSeverityLabel("中");
            vo.setSeverityColor("#FF9800");
        } else {
            vo.setAlertType("DEMAND_SURGE");
            vo.setAlertTypeLabel("需求激增预警");
            vo.setSeverityLevel(3);
            vo.setSeverityLabel("高");
            vo.setSeverityColor("#F44336");
        }
        return vo;
    }

    @Override
    public void markRead(Long userId, Long notificationId) {
        SysNotification n = sysNotificationMapper.selectById(notificationId);
        if (n == null || !n.getRecipientUserId().equals(userId)) {
            return;
        }
        n.setReadStatus(Constants.NOTIFICATION_READ);
        sysNotificationMapper.updateById(n);
    }

    @Override
    public void markAllRead(Long userId, Byte category) {
        LambdaUpdateWrapper<SysNotification> uw = new LambdaUpdateWrapper<>();
        uw.eq(SysNotification::getRecipientUserId, userId)
                .eq(SysNotification::getReadStatus, Constants.NOTIFICATION_UNREAD)
                .set(SysNotification::getReadStatus, Constants.NOTIFICATION_READ);
        if (category != null) {
            uw.eq(SysNotification::getMsgCategory, category);
        }
        sysNotificationMapper.update(null, uw);
    }
}
