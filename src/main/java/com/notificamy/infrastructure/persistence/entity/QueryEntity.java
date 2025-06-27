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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
    private String prompt;
    
    @Column(name = "is_valid")
    private Boolean isValid = false;
    
    @Column(name = "cron_params", length = 100)
    private String cronParams;
    
    @Column(name = "next_execution")
    private LocalDateTime nextExecution;
    
    @Column(name = "specific_datetime")
    private LocalDateTime specificDatetime;
    
    @Column(name = "closed")
    private Boolean closed = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "enabled_channels", columnDefinition = "TEXT")
    private String enabledChannels; // JSON string of enabled channels
    
    // New validation fields
    @Column(name = "out_of_bounds_prompt_length")
    private Boolean outOfBoundsPromptLength;
    
    @Column(name = "offensive_language_detected")
    private Boolean offensiveLanguageDetected;
    
    @Column(name = "nasty_instruction_detected")
    private Boolean nastyInstructionDetected;
    
    @Column(name = "purpose_valid")
    private Boolean purposeValid;
    
    @Column(name = "reasonable_usage")
    private Boolean reasonableUsage;
    
    @Column(name = "self_enforcing")
    private Boolean selfEnforcing;
    
    @Column(name = "invalid_reason", columnDefinition = "TEXT")
    private String invalidReason;
    
    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;
    
    @Column(name = "language", length = 10)
    private String language;
    
    @Column(name = "category", length = 50)
    private String category;
    
    @Column(name = "model_version", length = 50)
    private String modelVersion;
    
    @Column(name = "confidence_score")
    private Double confidenceScore;
    
    @Column(name = "policy_enforced")
    private Boolean policyEnforced;
    
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;
    
    // Query type flags
    @Column(name = "cron")
    private Boolean cron = false;
    
    @Column(name = "date_specific")
    private Boolean dateSpecific = false;
    
    @Column(name = "to_check")
    private Boolean toCheck = false;
    
    // Validity period
    @Column(name = "valid_from")
    private LocalDateTime validFrom;
    
    @Column(name = "valid_to")
    private LocalDateTime validTo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserEntity user;
}