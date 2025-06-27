package com.notificamy.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class NotificationRequest {
    
    private final Long queryId;
    private final String prompt;
    private final User user;
    private final Set<NotificationChannel> channels;
    private final String aiResponse;
}