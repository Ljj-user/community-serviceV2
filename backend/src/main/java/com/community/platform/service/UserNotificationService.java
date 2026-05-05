package com.community.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.community.platform.dto.MobileAlertCardVO;
import com.community.platform.dto.NotificationUnreadCountVO;
import com.community.platform.dto.UserNotificationVO;
import com.community.platform.generated.entity.Announcement;
import com.community.platform.generated.entity.ServiceClaim;
import com.community.platform.generated.entity.ServiceRequest;

public interface UserNotificationService {

    void notifyRequestAudited(ServiceRequest request, boolean approved, String rejectReason);

    void notifyServiceClaimed(ServiceRequest request, ServiceClaim claim);

    void broadcastAnnouncement(Announcement announcement);

    NotificationUnreadCountVO countUnread(Long userId);

    IPage<UserNotificationVO> pageMine(Long userId, Byte category, long page, long size);

    IPage<MobileAlertCardVO> pageMyAlertNotifications(Long userId, long page, long size, Boolean unreadOnly);

    long countMyAlertUnread(Long userId);

    void markRead(Long userId, Long notificationId);

    void markAllRead(Long userId, Byte category);
}
