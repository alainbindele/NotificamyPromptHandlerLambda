package com.notificamy.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@AllArgsConstructor
public class Query {
    
    private final Long id;
    private final Long userId;
    private final String prompt;
    private final Boolean isValid;
    private final String cronParams;
    private final LocalDateTime nextExecution;
    private final LocalDateTime createdAt;
    private final Set<NotificationChannel> enabledChannels;
}