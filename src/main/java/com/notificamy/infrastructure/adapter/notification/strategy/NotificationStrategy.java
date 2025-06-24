package com.notificamy.infrastructure.adapter.notification.strategy;

import com.notificamy.domain.model.NotificationRequest;

public interface NotificationStrategy {
    void sendNotification(NotificationRequest request);
}