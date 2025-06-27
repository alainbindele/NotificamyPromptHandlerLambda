package com.notificamy.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class UserEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "whatsapp_phone")
    private String whatsappPhone;
    
    @Column(name = "slack_webhook")
    private String slackWebhook;
    
    @Column(name = "discord_webhook")
    private String discordWebhook;
    
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<QueryEntity> queries;
}