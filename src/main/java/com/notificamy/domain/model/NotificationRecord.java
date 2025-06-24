package com.notificamy.domain.model;

import java.time.LocalDateTime;
import java.util.Set;

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

    public NotificationRecord(Long id, Long userId, Long queryId, LocalDateTime sentAt, 
                            NotificationStatus status, String subject, String content,
                            Set<NotificationChannel> channelsAttempted, 
                            Set<NotificationChannel> channelsSuccessful,
                            String errorMessage, Integer retryCount, 
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.queryId = queryId;
        this.sentAt = sentAt;
        this.status = status;
        this.subject = subject;
        this.content = content;
        this.channelsAttempted = channelsAttempted;
        this.channelsSuccessful = channelsSuccessful;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getQueryId() { return queryId; }
    public LocalDateTime getSentAt() { return sentAt; }
    public NotificationStatus getStatus() { return status; }
    public String getSubject() { return subject; }
    public String getContent() { return content; }
    public Set<NotificationChannel> getChannelsAttempted() { return channelsAttempted; }
    public Set<NotificationChannel> getChannelsSuccessful() { return channelsSuccessful; }
    public String getErrorMessage() { return errorMessage; }
    public Integer getRetryCount() { return retryCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}