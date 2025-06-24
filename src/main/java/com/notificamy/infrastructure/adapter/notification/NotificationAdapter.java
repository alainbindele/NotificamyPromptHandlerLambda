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
        NotificationDecorator decorator = buildDecoratorChain(request);
        
        if (decorator != null) {
            decorator.sendNotification(request);
        } else {
            LOG.warnf("No notification channels enabled for query %d", request.getQueryId());
        }
    }
    
    private NotificationDecorator buildDecoratorChain(NotificationRequest request) {
        NotificationDecorator chain = null;
        
        for (NotificationChannel channel : request.getChannels()) {
            NotificationDecorator strategy = getStrategyForChannel(channel);
            if (strategy != null) {
                if (chain == null) {
                    chain = strategy;
                } else {
                    chain = new NotificationDecorator(strategy, chain);
                }
            }
        }
        
        return chain;
    }
    
    private NotificationDecorator getStrategyForChannel(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> new NotificationDecorator(emailStrategy, null);
            case WHATSAPP -> new NotificationDecorator(whatsAppStrategy, null);
            case SLACK -> new NotificationDecorator(slackStrategy, null);
            case DISCORD -> new NotificationDecorator(discordStrategy, null);
        };
    }
}