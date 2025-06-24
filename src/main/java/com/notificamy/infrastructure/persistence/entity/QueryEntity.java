package com.notificamy.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "queries")
public class QueryEntity {
    
    @Id
    @Column(name = "id")
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
    private String prompt;
    
    @Column(name = "is_valid")
    private Boolean isValid = false;
    
    @Column(name = "cron_params", length = 30)
    private String cronParams;
    
    @Column(name = "next_execution")
    private LocalDateTime nextExecution;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "enabled_channels")
    private String enabledChannels; // JSON string of enabled channels
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserEntity user;

    // Constructors
    public QueryEntity() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public Boolean getIsValid() { return isValid; }
    public void setIsValid(Boolean isValid) { this.isValid = isValid; }

    public String getCronParams() { return cronParams; }
    public void setCronParams(String cronParams) { this.cronParams = cronParams; }

    public LocalDateTime getNextExecution() { return nextExecution; }
    public void setNextExecution(LocalDateTime nextExecution) { this.nextExecution = nextExecution; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getEnabledChannels() { return enabledChannels; }
    public void setEnabledChannels(String enabledChannels) { this.enabledChannels = enabledChannels; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
}