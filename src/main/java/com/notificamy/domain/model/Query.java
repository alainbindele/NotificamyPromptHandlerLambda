package com.notificamy.domain.model;

import java.time.LocalDateTime;
import java.util.Set;

public class Query {
    
    private final Long id;
    private final Long userId;
    private final String prompt;
    private final Boolean isValid;
    private final String cronParams;
    private final LocalDateTime nextExecution;
    private final LocalDateTime createdAt;
    private final Set<NotificationChannel> enabledChannels;

    public Query(Long id, Long userId, String prompt, Boolean isValid, String cronParams, 
                LocalDateTime nextExecution, LocalDateTime createdAt, Set<NotificationChannel> enabledChannels) {
        this.id = id;
        this.userId = userId;
        this.prompt = prompt;
        this.isValid = isValid;
        this.cronParams = cronParams;
        this.nextExecution = nextExecution;
        this.createdAt = createdAt;
        this.enabledChannels = enabledChannels;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getPrompt() { return prompt; }
    public Boolean getIsValid() { return isValid; }
    public String getCronParams() { return cronParams; }
    public LocalDateTime getNextExecution() { return nextExecution; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Set<NotificationChannel> getEnabledChannels() { return enabledChannels; }
}