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
        LOG.infof("Sending notifications for query %d through %d enabled channels: %s", 
                request.getQueryId(), request.getChannels().size(), request.getChannels());
        
        List<Exception> exceptions = new ArrayList<>();
        List<NotificationChannel> successfulChannels = new ArrayList<>();
        List<NotificationChannel> failedChannels = new ArrayList<>();
        
        for (NotificationChannel channel : request.getChannels()) {
            LOG.infof("Attempting to send notification via %s for query %d", channel, request.getQueryId());
            try {
                sendToChannel(channel, request);
                successfulChannels.add(channel);
                LOG.infof("Successfully sent notification via %s for query %d", channel, request.getQueryId());
            } catch (Exception e) {
                failedChannels.add(channel);
                exceptions.add(e);
                LOG.errorf(e, "Failed to send notification via %s for query %d: %s", 
                        channel, request.getQueryId(), e.getMessage());
            }
        }
        
        LOG.infof("Notification sending completed for query %d: %d successful, %d failed", 
                request.getQueryId(), successfulChannels.size(), failedChannels.size());
        
        if (!successfulChannels.isEmpty()) {
            LOG.infof("Successful channels for query %d: %s", request.getQueryId(), successfulChannels);
        }
        if (!failedChannels.isEmpty()) {
            LOG.warnf("Failed channels for query %d: %s", request.getQueryId(), failedChannels);
        }
        
        // Se tutti i canali sono falliti, lanciamo un'eccezione
        if (successfulChannels.isEmpty() && !failedChannels.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("All notification channels failed: ");
            for (int i = 0; i < failedChannels.size(); i++) {
                errorMessage.append(failedChannels.get(i));
                if (i < failedChannels.size() - 1) {
                    errorMessage.append(", ");
                }
            }
            throw new RuntimeException(errorMessage.toString(), exceptions.get(0));
        }
        
        // Se alcuni canali sono falliti ma almeno uno è riuscito, logghiamo ma non lanciamo eccezione
        if (!failedChannels.isEmpty()) {
            LOG.warnf("Some notification channels failed for query %d: %s", 
                    request.getQueryId(), failedChannels);
        }
    }
    
    private void sendToChannel(NotificationChannel channel, NotificationRequest request) {
        LOG.debugf("Routing notification to %s strategy for query %d", channel, request.getQueryId());
        
        switch (channel) {
            case EMAIL -> emailStrategy.sendNotification(request);
            case WHATSAPP -> whatsAppStrategy.sendNotification(request);
            case SLACK -> slackStrategy.sendNotification(request);
            case DISCORD -> discordStrategy.sendNotification(request);
            default -> {
                LOG.warnf("Unknown notification channel: %s", channel);
                throw new RuntimeException("Unknown notification channel: " + channel);
            }
        }
    }
}