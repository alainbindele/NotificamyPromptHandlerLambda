package com.notificamy.domain.service;

import com.notificamy.domain.model.NotificationRequest;
import com.notificamy.domain.model.Query;
import com.notificamy.domain.model.User;
import com.notificamy.domain.port.AiServicePort;
import com.notificamy.domain.port.NotificationPort;
import com.notificamy.domain.port.QueryRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NotificationService {
    
    private static final Logger LOG = Logger.getLogger(NotificationService.class);
    
    @Inject
    QueryRepositoryPort queryRepository;
    
    @Inject
    AiServicePort aiService;
    
    @Inject
    NotificationPort notificationPort;
    
    @Transactional
    public void processNotificationRequest(Long queryId, String prompt) {
        LOG.infof("Processing notification request for query ID: %d", queryId);
        
        try {
            // Fetch query and user
            Query query = queryRepository.findById(queryId);
            if (query == null) {
                throw new RuntimeException("Query not found: " + queryId);
            }
            
            User user = queryRepository.findUserById(query.getUserId());
            if (user == null) {
                throw new RuntimeException("User not found for query: " + queryId);
            }
            
            // Process with AI
            String aiResponse = aiService.processPrompt(prompt);
            
            // Create notification request
            NotificationRequest notificationRequest = new NotificationRequest(
                    queryId, prompt, user, query.getEnabledChannels(), aiResponse
            );
            
            // Send notifications through all enabled channels
            notificationPort.sendNotification(notificationRequest);
            
            LOG.infof("Notification request processed successfully for query ID: %d", queryId);
            
        } catch (Exception e) {
            LOG.errorf(e, "Error processing notification request for query ID: %d", queryId);
            throw new RuntimeException("Failed to process notification request", e);
        }
    }
}