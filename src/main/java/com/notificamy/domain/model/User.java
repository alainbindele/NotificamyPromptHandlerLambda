package com.notificamy.domain.model;

import java.time.LocalDateTime;
import java.util.Map;

public class User {
    
    private final Long id;
    private final String email;
    private final String name;
    private final LocalDateTime createdAt;
    private final Map<NotificationChannel, String> channelConfigurations;

    public User(Long id, String email, String name, LocalDateTime createdAt, Map<NotificationChannel, String> channelConfigurations) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.createdAt = createdAt;
        this.channelConfigurations = channelConfigurations;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Map<NotificationChannel, String> getChannelConfigurations() { return channelConfigurations; }
    
    public String getChannelConfiguration(NotificationChannel channel) {
        return channelConfigurations.get(channel);
    }
}