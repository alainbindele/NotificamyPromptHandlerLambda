package com.notificamy.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class User {
    
    private final Long id;
    private final String email;
    private final String name;
    private final LocalDateTime createdAt;
    private final Map<NotificationChannel, String> channelConfigurations;
    
    public String getChannelConfiguration(NotificationChannel channel) {
        return channelConfigurations.get(channel);
    }
}