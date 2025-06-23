package com.notificamy.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "queries")
@Data
@NoArgsConstructor
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
}