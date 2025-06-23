package com.notificamy.infrastructure.adapter.notification;

import com.notificamy.domain.model.NotificationChannel;
import com.notificamy.domain.model.NotificationRequest;
import com.notificamy.domain.port.NotificationPort;
import com.notificamy.infrastructure.adapter.notification.decorator.NotificationDecorator;
import com.notificamy.infrastructure.adapter.notification.strategy.DiscordNotificationStrategy;
import com.notificamy.infrastructure.adapter.notification.strategy.EmailNotificationStrategy;
import com.notificamy.infrastructure.adapter.notification.strategy.SlackNotificationStrategy;
import com.notificamy.infrastructure.adapter.notification.strategy.WhatsAppNotificationStrategy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

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
        
        // Build decorator chain based on enabled channels
        NotificationDecorator decoratorChain = buildDecoratorChain(request);
        
        if (decoratorChain != null) {
            decoratorChain.sendNotification(request);
            LOG.infof("All notifications sent successfully for query %d", request.getQueryId());
        } else {
            LOG.warnf("No notification channels enabled for query %d", request.getQueryId());
        }
    }
    
    private NotificationDecorator buildDecoratorChain(NotificationRequest request) {
        NotificationDecorator chain = null;
        
        // Build chain in reverse order so the first channel added becomes the last executed
        // This ensures proper decorator pattern execution order
        
        if (request.getChannels().contains(NotificationChannel.DISCORD)) {
            chain = new NotificationDecorator(discordStrategy, chain);
            LOG.debugf("Added Discord to notification chain for query %d", request.getQueryId());
        }
        
        if (request.getChannels().contains(NotificationChannel.SLACK)) {
            chain = new NotificationDecorator(slackStrategy, chain);
            LOG.debugf("Added Slack to notification chain for query %d", request.getQueryId());
        }
        
        if (request.getChannels().contains(NotificationChannel.WHATSAPP)) {
            chain = new NotificationDecorator(whatsAppStrategy, chain);
            LOG.debugf("Added WhatsApp to notification chain for query %d", request.getQueryId());
        }
        
        if (request.getChannels().contains(NotificationChannel.EMAIL)) {
            chain = new NotificationDecorator(emailStrategy, chain);
            LOG.debugf("Added Email to notification chain for query %d", request.getQueryId());
        }
        
        return chain;
    }
}