package com.notificamy.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private Long queryId;
    private String prompt;
    private User user;
    private Set<NotificationChannel> channels;
    private String aiResponse;
}