package com.notificamy.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Query {
    private Long id;
    private Long userId;
    private String prompt;
    private Boolean isValid;
    private String cronParams;
    private LocalDateTime nextExecution;
    private LocalDateTime createdAt;
    private Set<NotificationChannel> enabledChannels;
}