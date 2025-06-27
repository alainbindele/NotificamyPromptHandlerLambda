package com.notificamy.domain.service;

import com.notificamy.domain.model.*;
import com.notificamy.domain.port.AiServicePort;
import com.notificamy.domain.port.NotificationPort;
import com.notificamy.domain.port.NotificationRecordPort;
import com.notificamy.domain.port.QueryRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@ApplicationScoped
public class NotificationService {
    
    private static final Logger LOG = Logger.getLogger(NotificationService.class);
    
    // Pattern to match <checked>true</checked> tag in AI response
    private static final Pattern CHECKED_TRUE_PATTERN = Pattern.compile("<checked>\\s*true\\s*</checked>", Pattern.CASE_INSENSITIVE);
    
    @Inject
    QueryRepositoryPort queryRepository;
    
    @Inject
    AiServicePort aiService;
    
    @Inject
    NotificationPort notificationPort;
    
    @Inject
    NotificationRecordPort notificationRecordPort;
    
    @Transactional
    public void processNotificationRequest(Long queryId, String prompt) {
        LOG.infof("Processing notification request for query ID: %d", queryId);
        
        NotificationRecord notificationRecord = null;
        
        try {
            // Fetch query and user
            Query query = queryRepository.findById(queryId);
            if (query == null) {
                throw new RuntimeException("Query not found: " + queryId);
            }
            
            LocalDateTime now = LocalDateTime.now();
            boolean queryUpdated = false;
            
            // Check if date-specific query has expired and close it if needed
            if (Boolean.TRUE.equals(query.getDateSpecific()) && query.getNextExecution() != null) {
                if (query.getNextExecution().isBefore(now)) {
                    LOG.infof("Date-specific query %d has expired (next_execution: %s), closing it", 
                            queryId, query.getNextExecution());
                    queryRepository.updateQueryClosed(queryId, true);
                    queryUpdated = true;
                }
            }
            
            // Check if query is beyond valid_to period and close it if needed
            if (query.getValidTo() != null && now.isAfter(query.getValidTo())) {
                LOG.infof("Query %d is beyond valid_to period (%s), closing it", 
                        queryId, query.getValidTo());
                queryRepository.updateQueryClosed(queryId, true);
                queryUpdated = true;
            }
            
            // Refresh the query if it was updated
            if (queryUpdated) {
                query = queryRepository.findById(queryId);
                if (query == null) {
                    throw new RuntimeException("Query not found after update: " + queryId);
                }
            }
            
            // Check if query is active
            if (!query.isActive()) {
                LOG.infof("Query %d is not active (valid: %s, closed: %s), skipping notification", 
                        queryId, query.getIsValid(), query.getClosed());
                return;
            }
            
            // Check validity period
            if (!query.isWithinValidityPeriod()) {
                LOG.infof("Query %d is outside validity period (from: %s, to: %s), skipping notification", 
                        queryId, query.getValidFrom(), query.getValidTo());
                return;
            }
            
            User user = queryRepository.findUserById(query.getUserId());
            if (user == null) {
                throw new RuntimeException("User not found for query: " + queryId);
            }
            
            // For conditional queries (to_check = true), check temporal constraints first
            if (query.requiresConditionalCheck()) {
                if (!isWithinTemporalConstraints(query, now)) {
                    LOG.infof("Query %d is outside temporal constraints, skipping conditional check", queryId);
                    return;
                }
            }
            
            // Process with AI (include language specification in prompt)
            String enhancedPrompt = buildLanguageSpecificPrompt(prompt, query.getLanguage());
            String aiResponse = aiService.processPrompt(enhancedPrompt);
            
            // For conditional queries (to_check = true), check if notification should be sent
            if (query.requiresConditionalCheck()) {
                if (!shouldSendConditionalNotification(aiResponse)) {
                    LOG.infof("Conditional check failed for query %d, not sending notification", queryId);
                    return;
                }
                LOG.infof("Conditional check passed for query %d, proceeding with notification", queryId);
                
                // For one-time conditional events, close the query after successful notification
                if (isOneTimeConditionalEvent(query)) {
                    LOG.infof("One-time conditional event completed for query %d, closing query", queryId);
                    queryRepository.updateQueryClosed(queryId, true);
                }
            }
            
            // Create notification request
            NotificationRequest notificationRequest = new NotificationRequest(
                    queryId, prompt, user, query.getEnabledChannels(), aiResponse
            );
            
            // Create notification record for tracking
            notificationRecord = notificationRecordPort.createNotificationRecord(notificationRequest);
            
            // Send notifications through all enabled channels
            Set<NotificationChannel> successfulChannels = new HashSet<>();
            Set<NotificationChannel> failedChannels = new HashSet<>();
            StringBuilder errorMessages = new StringBuilder();
            
            try {
                notificationPort.sendNotification(notificationRequest);
                
                // Se arriviamo qui, significa che almeno un canale è riuscito
                // Per ora assumiamo che tutti i canali siano riusciti se non c'è eccezione
                // In futuro, il NotificationAdapter potrebbe restituire informazioni più dettagliate
                successfulChannels.addAll(query.getEnabledChannels());
                
            } catch (Exception e) {
                LOG.errorf(e, "Error sending notifications for query %d", queryId);
                
                // Se c'è un'eccezione, significa che tutti i canali sono falliti
                failedChannels.addAll(query.getEnabledChannels());
                errorMessages.append("Notification sending failed: ").append(e.getMessage());
            }
            
            // Update notification record with results
            NotificationStatus finalStatus = determineFinalStatus(successfulChannels, failedChannels, query.getEnabledChannels());
            notificationRecordPort.updateNotificationStatus(
                    notificationRecord.getId(), 
                    finalStatus, 
                    successfulChannels, 
                    errorMessages.length() > 0 ? errorMessages.toString() : null
            );
            
            LOG.infof("Notification request processed for query ID: %d with status: %s", queryId, finalStatus);
            
        } catch (Exception e) {
            LOG.errorf(e, "Error processing notification request for query ID: %d", queryId);
            
            // Update notification record with error status if it was created
            if (notificationRecord != null) {
                try {
                    notificationRecordPort.updateNotificationStatus(
                            notificationRecord.getId(), 
                            NotificationStatus.ERROR, 
                            Set.of(), 
                            "Processing failed: " + e.getMessage()
                    );
                } catch (Exception updateException) {
                    LOG.errorf(updateException, "Failed to update notification record %d with error status", 
                            notificationRecord.getId());
                }
            }
            
            // Non rilanciamo l'eccezione per evitare che il Lambda fallisca completamente
            LOG.errorf("Notification processing failed for query %d, but continuing execution", queryId);
        }
    }
    
    /**
     * Checks if the current time is within the temporal constraints for conditional queries
     */
    private boolean isWithinTemporalConstraints(Query query, LocalDateTime now) {
        // If no cron parameters, no temporal constraints
        if (query.getCronParams() == null || query.getCronParams().isEmpty()) {
            return true;
        }
        
        String cronParams = query.getCronParams();
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek currentDay = now.getDayOfWeek();
        
        // Parse common cron patterns for temporal constraints
        try {
            String[] cronParts = cronParams.split("\\s+");
            if (cronParts.length >= 5) {
                String minute = cronParts[0];
                String hour = cronParts[1];
                String dayOfMonth = cronParts[2];
                String month = cronParts[3];
                String dayOfWeek = cronParts[4];
                
                // Check hour constraint
                if (!hour.equals("*")) {
                    if (hour.contains("-")) {
                        // Range like "9-17"
                        String[] hourRange = hour.split("-");
                        int startHour = Integer.parseInt(hourRange[0]);
                        int endHour = Integer.parseInt(hourRange[1]);
                        int currentHour = currentTime.getHour();
                        
                        if (currentHour < startHour || currentHour > endHour) {
                            LOG.infof("Current hour %d is outside range %d-%d", currentHour, startHour, endHour);
                            return false;
                        }
                    } else {
                        // Specific hour like "9"
                        int targetHour = Integer.parseInt(hour);
                        if (currentTime.getHour() != targetHour) {
                            LOG.infof("Current hour %d does not match target hour %d", currentTime.getHour(), targetHour);
                            return false;
                        }
                    }
                }
                
                // Check day of week constraint (0 = Sunday, 1 = Monday, etc.)
                if (!dayOfWeek.equals("*")) {
                    if (dayOfWeek.contains(",")) {
                        // Multiple days like "1,2,3,4,5" (weekdays)
                        String[] days = dayOfWeek.split(",");
                        boolean dayMatches = false;
                        for (String day : days) {
                            int targetDay = Integer.parseInt(day.trim());
                            // Convert Java DayOfWeek to cron format (Monday=1 becomes 1, Sunday=7 becomes 0)
                            int currentDayValue = currentDay.getValue() % 7; // Sunday becomes 0
                            if (currentDayValue == targetDay) {
                                dayMatches = true;
                                break;
                            }
                        }
                        if (!dayMatches) {
                            LOG.infof("Current day %s does not match any target days %s", currentDay, dayOfWeek);
                            return false;
                        }
                    } else {
                        // Single day
                        int targetDay = Integer.parseInt(dayOfWeek);
                        int currentDayValue = currentDay.getValue() % 7; // Sunday becomes 0
                        if (currentDayValue != targetDay) {
                            LOG.infof("Current day %s does not match target day %d", currentDay, targetDay);
                            return false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warnf(e, "Failed to parse cron parameters: %s, allowing execution", cronParams);
            return true; // If we can't parse, allow execution
        }
        
        LOG.infof("Temporal constraints satisfied for query %d", query.getId());
        return true;
    }
    
    /**
     * Determines if this is a one-time conditional event that should close the query after success
     */
    private boolean isOneTimeConditionalEvent(Query query) {
        // If it's not a recurring cron job and it's a conditional check, it's likely one-time
        if (Boolean.TRUE.equals(query.getToCheck()) && !Boolean.TRUE.equals(query.getCron())) {
            return true;
        }
        
        // Check for specific patterns in the prompt that indicate one-time events
        String prompt = query.getPrompt().toLowerCase();
        
        // Patterns that suggest one-time events
        String[] oneTimePatterns = {
            "avvisami quando",
            "dimmi quando",
            "notificami quando",
            "alert me when",
            "notify me when",
            "tell me when"
        };
        
        for (String pattern : oneTimePatterns) {
            if (prompt.contains(pattern)) {
                // But exclude patterns that suggest recurring checks
                String[] recurringPatterns = {
                    "ogni giorno",
                    "ogni mattina",
                    "ogni sera",
                    "ogni settimana",
                    "ogni lunedì",
                    "ogni martedì",
                    "ogni mercoledì",
                    "ogni giovedì",
                    "ogni venerdì",
                    "ogni sabato",
                    "ogni domenica",
                    "daily",
                    "every day",
                    "every morning",
                    "every evening",
                    "every week"
                };
                
                boolean isRecurring = false;
                for (String recurringPattern : recurringPatterns) {
                    if (prompt.contains(recurringPattern)) {
                        isRecurring = true;
                        break;
                    }
                }
                
                if (!isRecurring) {
                    LOG.infof("Detected one-time conditional event pattern: %s", pattern);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Builds a language-specific prompt by appending language instruction to the original prompt
     */
    private String buildLanguageSpecificPrompt(String originalPrompt, String language) {
        if (language == null || language.isEmpty()) {
            return originalPrompt;
        }
        
        String languageInstruction = switch (language.toLowerCase()) {
            case "it", "italian" -> "Rispondi specificatamente in italiano.";
            case "en", "english" -> "Respond specifically in English.";
            case "es", "spanish" -> "Responde específicamente en español.";
            case "fr", "french" -> "Répondez spécifiquement en français.";
            case "de", "german" -> "Antworten Sie spezifisch auf Deutsch.";
            default -> String.format("Respond specifically in %s language.", language);
        };
        
        return originalPrompt + " " + languageInstruction;
    }
    
    /**
     * Checks if a conditional notification should be sent based on AI response
     * Returns true if the AI response contains <checked>true</checked> tag
     */
    private boolean shouldSendConditionalNotification(String aiResponse) {
        if (aiResponse == null || aiResponse.isEmpty()) {
            return false;
        }
        
        return CHECKED_TRUE_PATTERN.matcher(aiResponse).find();
    }
    
    private NotificationStatus determineFinalStatus(Set<NotificationChannel> successful, 
                                                  Set<NotificationChannel> failed, 
                                                  Set<NotificationChannel> attempted) {
        if (successful.isEmpty() && !failed.isEmpty()) {
            return NotificationStatus.FAILED;
        } else if (!successful.isEmpty() && failed.isEmpty()) {
            return NotificationStatus.SUCCESS;
        } else if (!successful.isEmpty() && !failed.isEmpty()) {
            return NotificationStatus.PARTIAL;
        } else {
            return NotificationStatus.ERROR;
        }
    }
}