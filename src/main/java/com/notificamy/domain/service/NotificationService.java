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
            
            // Check if date-specific query has expired and close it if needed
            if (Boolean.TRUE.equals(query.getDateSpecific()) && query.getNextExecution() != null) {
                LocalDateTime now = LocalDateTime.now();
                if (query.getNextExecution().isBefore(now)) {
                    LOG.infof("Date-specific query %d has expired (next_execution: %s), closing it", 
                            queryId, query.getNextExecution());
                    queryRepository.updateQueryClosed(queryId, true);
                    
                    // Refresh the query to get updated status
                    query = queryRepository.findById(queryId);
                    if (query == null) {
                        throw new RuntimeException("Query not found after update: " + queryId);
                    }
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