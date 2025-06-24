package com.notificamy.domain.port;

import com.notificamy.domain.model.NotificationRequest;

public interface NotificationPort {
    void sendNotification(NotificationRequest request);
}