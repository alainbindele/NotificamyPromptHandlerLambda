package com.notificamy.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
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

    // Constructors
    public UserEntity() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getWhatsappPhone() { return whatsappPhone; }
    public void setWhatsappPhone(String whatsappPhone) { this.whatsappPhone = whatsappPhone; }

    public String getSlackWebhook() { return slackWebhook; }
    public void setSlackWebhook(String slackWebhook) { this.slackWebhook = slackWebhook; }

    public String getDiscordWebhook() { return discordWebhook; }
    public void setDiscordWebhook(String discordWebhook) { this.discordWebhook = discordWebhook; }

    public List<QueryEntity> getQueries() { return queries; }
    public void setQueries(List<QueryEntity> queries) { this.queries = queries; }
}