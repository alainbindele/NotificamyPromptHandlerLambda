package com.notificamy.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@AllArgsConstructor
public class NotificationRecord {
    
    private final Long id;
    private final Long userId;
    private final Long queryId;
    private final LocalDateTime sentAt;
    private final NotificationStatus status;
    private final String subject;
    private final String content;
    private final Set<NotificationChannel> channelsAttempted;
    private final Set<NotificationChannel> channelsSuccessful;
    private final String errorMessage;
    private final Integer retryCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}