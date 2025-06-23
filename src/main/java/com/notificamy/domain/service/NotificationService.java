package com.notificamy.domain.service;

import com.notificamy.domain.model.NotificationRequest;
import com.notificamy.domain.model.User;
import com.notificamy.domain.model.NotificationChannel;
import com.notificamy.domain.port.AiServicePort;
import com.notificamy.domain.port.NotificationPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Set;

@ApplicationScoped
public class NotificationService {
    
    private static final Logger LOG = Logger.getLogger(NotificationService.class);
    
    @Inject
    AiServicePort aiService;
    
    @Inject
    NotificationPort notificationPort;
    
    public void processNotificationRequest(Long queryId, String prompt, User user, Set<NotificationChannel> enabledChannels) {
        LOG.infof("Processing notification request for query ID: %d with %d channels", queryId, enabledChannels.size());
        
        // Validate input
        if (enabledChannels == null || enabledChannels.isEmpty()) {
            throw new RuntimeException("No notification channels enabled for query: " + queryId);
        }
        
        // Process with AI
        String aiResponse = aiService.processPrompt(prompt);
        
        // Create notification request
        NotificationRequest notificationRequest = new NotificationRequest(
                queryId, prompt, user, enabledChannels, aiResponse
        );
        
        // Send notifications through all enabled channels using Decorator pattern
        notificationPort.sendNotification(notificationRequest);
        
        LOG.infof("Notification request processed successfully for query ID: %d through %d channels", 
                queryId, enabledChannels.size());
    }
}