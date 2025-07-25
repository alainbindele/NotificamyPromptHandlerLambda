package com.notificamy.infrastructure.adapter.notification;

import com.notificamy.domain.model.NotificationChannel;
import com.notificamy.domain.model.NotificationRequest;
import com.notificamy.domain.port.NotificationPort;
import com.notificamy.infrastructure.adapter.notification.strategy.DiscordNotificationStrategy;
import com.notificamy.infrastructure.adapter.notification.strategy.EmailNotificationStrategy;
import com.notificamy.infrastructure.adapter.notification.strategy.SlackNotificationStrategy;
import com.notificamy.infrastructure.adapter.notification.strategy.WhatsAppNotificationStrategy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class NotificationAdapter implements NotificationPort {
    
    private static final Logger LOG = Logger.getLogger(NotificationAdapter.class);
    
    @Inject
    EmailNotificationStrategy emailStrategy;
    
    @Inject
    WhatsAppNotificationStrategy whatsAppStrategy;
    
    @Inject
    SlackNotificationStrategy slackStrategy;
    
    @Inject
    DiscordNotificationStrategy discordStrategy;
    
    @Override
    public void sendNotification(NotificationRequest request) {
        LOG.infof("=== NOTIFICATION ADAPTER - STARTING MULTI-CHANNEL SEND ===");
        LOG.infof("Query ID: %d", request.getQueryId());
        LOG.infof("User ID: %d, Email: %s", request.getUser().getId(), request.getUser().getEmail());
        LOG.infof("Total enabled channels: %d", request.getChannels().size());
        LOG.infof("Enabled channels: %s", request.getChannels());
        LOG.infof("AI Response length: %d characters", request.getAiResponse().length());
        
        List<Exception> exceptions = new ArrayList<>();
        List<NotificationChannel> successfulChannels = new ArrayList<>();
        List<NotificationChannel> failedChannels = new ArrayList<>();
        
        for (NotificationChannel channel : request.getChannels()) {
            LOG.infof("--- ATTEMPTING CHANNEL: %s ---", channel);
            LOG.infof("Query ID: %d, Channel: %s", request.getQueryId(), channel);
            
            try {
                long startTime = System.currentTimeMillis();
                sendToChannel(channel, request);
                long endTime = System.currentTimeMillis();
                
                successfulChannels.add(channel);
                LOG.infof("✅ SUCCESS - Channel: %s, Query: %d, Duration: %dms", 
                        channel, request.getQueryId(), (endTime - startTime));
                        
            } catch (Exception e) {
                failedChannels.add(channel);
                exceptions.add(e);
                LOG.errorf("❌ FAILED - Channel: %s, Query: %d", channel, request.getQueryId());
                LOG.errorf("Error message: %s", e.getMessage());
                LOG.errorf("Exception type: %s", e.getClass().getSimpleName());
                if (e.getCause() != null) {
                    LOG.errorf("Caused by: %s - %s", e.getCause().getClass().getSimpleName(), e.getCause().getMessage());
                }
            }
        }
        
        LOG.infof("=== NOTIFICATION ADAPTER - MULTI-CHANNEL SEND COMPLETED ===");
        LOG.infof("Query ID: %d", request.getQueryId());
        LOG.infof("Total channels attempted: %d", request.getChannels().size());
        LOG.infof("Successful channels: %d - %s", successfulChannels.size(), successfulChannels);
        LOG.infof("Failed channels: %d - %s", failedChannels.size(), failedChannels);
        
        if (!successfulChannels.isEmpty()) {
            LOG.infof("✅ At least one channel succeeded - Operation considered successful");
        }
        if (!failedChannels.isEmpty()) {
            LOG.warnf("❌ Some channels failed: %s", failedChannels);
            for (int i = 0; i < failedChannels.size() && i < exceptions.size(); i++) {
                LOG.warnf("Channel %s failed with: %s", failedChannels.get(i), exceptions.get(i).getMessage());
            }
        }
        
        // Se tutti i canali sono falliti, lanciamo un'eccezione
        if (successfulChannels.isEmpty() && !failedChannels.isEmpty()) {
            LOG.errorf("🚨 ALL CHANNELS FAILED - Throwing exception");
            StringBuilder errorMessage = new StringBuilder("All notification channels failed: ");
            for (int i = 0; i < failedChannels.size(); i++) {
                errorMessage.append(failedChannels.get(i));
                if (i < failedChannels.size() - 1) {
                    errorMessage.append(", ");
                }
            }
            LOG.errorf("Combined error message: %s", errorMessage.toString());
            throw new RuntimeException(errorMessage.toString(), exceptions.get(0));
        }
        
        // Se alcuni canali sono falliti ma almeno uno è riuscito, logghiamo ma non lanciamo eccezione
        if (!failedChannels.isEmpty()) {
            LOG.warnf("⚠️ PARTIAL SUCCESS - Some channels failed but at least one succeeded");
            LOG.warnf("Query %d: Failed channels: %s", request.getQueryId(), failedChannels);
        }
    }
    
    private void sendToChannel(NotificationChannel channel, NotificationRequest request) {
        LOG.infof("🔀 ROUTING to %s strategy for query %d", channel, request.getQueryId());
        
        switch (channel) {
            case EMAIL -> emailStrategy.sendNotification(request);
            case WHATSAPP -> whatsAppStrategy.sendNotification(request);
            case SLACK -> slackStrategy.sendNotification(request);
            case DISCORD -> discordStrategy.sendNotification(request);
            default -> {
                LOG.errorf("❌ UNKNOWN CHANNEL: %s for query %d", channel, request.getQueryId());
                throw new RuntimeException("Unknown notification channel: " + channel);
            }
        }
    }
}