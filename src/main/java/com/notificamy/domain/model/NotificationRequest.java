package com.notificamy.domain.model;

import java.util.Set;

public class NotificationRequest {
    
    private final Long queryId;
    private final String prompt;
    private final User user;
    private final Set<NotificationChannel> channels;
    private final String aiResponse;

    public NotificationRequest(Long queryId, String prompt, User user, Set<NotificationChannel> channels, String aiResponse) {
        this.queryId = queryId;
        this.prompt = prompt;
        this.user = user;
        this.channels = channels;
        this.aiResponse = aiResponse;
    }

    public Long getQueryId() { return queryId; }
    public String getPrompt() { return prompt; }
    public User getUser() { return user; }
    public Set<NotificationChannel> getChannels() { return channels; }
    public String getAiResponse() { return aiResponse; }
}