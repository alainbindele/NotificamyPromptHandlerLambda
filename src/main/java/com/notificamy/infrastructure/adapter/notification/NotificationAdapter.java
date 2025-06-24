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
        LOG.infof("Sending notifications for query %d through %d channels", 
                request.getQueryId(), request.getChannels().size());
        
        List<Exception> exceptions = new ArrayList<>();
        int successCount = 0;
        
        for (NotificationChannel channel : request.getChannels()) {
            try {
                sendToChannel(channel, request);
                successCount++;
                LOG.infof("Successfully sent notification via %s for query %d", channel, request.getQueryId());
            } catch (Exception e) {
                LOG.errorf(e, "Failed to send notification via %s for query %d", channel, request.getQueryId());
                exceptions.add(e);
            }
        }
        
        LOG.infof("Notification sending completed for query %d: %d successful, %d failed", 
                request.getQueryId(), successCount, exceptions.size());
        
        // If all channels failed, throw an exception
        if (successCount == 0 && !exceptions.isEmpty()) {
            throw new RuntimeException("All notification channels failed", exceptions.get(0));
        }
    }
    
    private void sendToChannel(NotificationChannel channel, NotificationRequest request) {
        switch (channel) {
            case EMAIL -> emailStrategy.sendNotification(request);
            case WHATSAPP -> whatsAppStrategy.sendNotification(request);
            case SLACK -> slackStrategy.sendNotification(request);
            case DISCORD -> discordStrategy.sendNotification(request);
            default -> LOG.warnf("Unknown notification channel: %s", channel);
        }
    }
}