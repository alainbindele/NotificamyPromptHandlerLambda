package com.notificamy.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String email;
    private String name;
    private LocalDateTime createdAt;
    private Map<NotificationChannel, String> channelConfigurations;
    
    public String getChannelConfiguration(NotificationChannel channel) {
        return channelConfigurations != null ? channelConfigurations.get(channel) : null;
    }
}