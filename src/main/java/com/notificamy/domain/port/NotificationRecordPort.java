package com.notificamy.domain.port;

import com.notificamy.domain.model.NotificationRecord;
import com.notificamy.domain.model.NotificationRequest;
import com.notificamy.domain.model.NotificationStatus;
import com.notificamy.domain.model.NotificationChannel;

import java.util.Set;

public interface NotificationRecordPort {
    NotificationRecord createNotificationRecord(NotificationRequest request);
    void updateNotificationStatus(Long notificationId, NotificationStatus status, 
                                Set<NotificationChannel> successfulChannels, String errorMessage);
    void incrementRetryCount(Long notificationId);
}