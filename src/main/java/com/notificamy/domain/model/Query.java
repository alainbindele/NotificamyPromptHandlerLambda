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
    private final Boolean closed;
    private final LocalDateTime createdAt;
    private final Set<NotificationChannel> enabledChannels;
    
    // New validation fields
    private final Boolean outOfBoundsPromptLength;
    private final Boolean offensiveLanguageDetected;
    private final Boolean nastyInstructionDetected;
    private final Boolean purposeValid;
    private final Boolean reasonableUsage;
    private final Boolean selfEnforcing;
    private final String invalidReason;
    private final String summaryText;
    private final String language;
    private final String category;
    private final String modelVersion;
    private final Double confidenceScore;
    private final Boolean policyEnforced;
    private final String tags;
    
    // Query type flags
    private final Boolean cron;
    private final Boolean dateSpecific;
    private final Boolean toCheck;
    
    // Validity period
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;
    
    /**
     * Checks if the query should be executed based on its validity period
     */
    public boolean isWithinValidityPeriod() {
        LocalDateTime now = LocalDateTime.now();
        
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }
        
        if (validTo != null && now.isAfter(validTo)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if this is a conditional query that requires checking
     */
    public boolean requiresConditionalCheck() {
        return Boolean.TRUE.equals(toCheck);
    }
    
    /**
     * Checks if the query is active (valid and not closed)
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isValid) && !Boolean.TRUE.equals(closed);
    }
}